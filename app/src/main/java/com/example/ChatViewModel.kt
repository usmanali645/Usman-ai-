package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val conversations: List<ConversationEntity> = emptyList(),
    val currentConversationId: String? = null,
    val isLoading: Boolean = false,
    val pendingImageUri: Uri? = null,
    val pendingImageBase64: String? = null
)

class ChatViewModel(
    private val repository: ChatRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messageHistoryForApi = mutableListOf<Content>()
    val voiceResponseEnabled: StateFlow<Boolean> = settingsManager.voiceResponse

    private val _speakEvent = MutableSharedFlow<String>()
    val speakEvent = _speakEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.allConversations.collect { convs ->
                _uiState.update { it.copy(conversations = convs) }
            }
        }
    }

    fun startNewChat() {
        _uiState.update { it.copy(
            currentConversationId = null,
            messages = emptyList()
        ) }
        messageHistoryForApi.clear()
    }

    fun loadConversation(id: String) {
        _uiState.update { it.copy(currentConversationId = id) }
        viewModelScope.launch {
            repository.getMessagesForConversation(id).collect { msgs ->
                if (_uiState.value.currentConversationId == id) {
                    _uiState.update { it.copy(messages = msgs) }
                    messageHistoryForApi = msgs.takeLast(20).mapNotNull { entity ->
                        if (entity.isError) null
                        else Content(parts = listOf(Part(text = entity.text)))
                    }.toMutableList()
                }
            }
        }
    }
    
    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_uiState.value.currentConversationId == id) {
                startNewChat()
            }
        }
    }

    fun setPendingImage(context: Context, uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(pendingImageUri = null, pendingImageBase64 = null) }
            return
        }
        _uiState.update { it.copy(pendingImageUri = uri) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                _uiState.update { it.copy(pendingImageBase64 = base64) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(pendingImageUri = null, pendingImageBase64 = null) }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            startNewChat()
        }
    }

    fun sendMessage(text: String) {
        val imageBase64 = _uiState.value.pendingImageBase64
        val displayMessage = if (imageBase64 != null) "$text [Image attached]" else text

        if (text.isBlank() && imageBase64 == null) return
        
        _uiState.update { it.copy(
            isLoading = true,
            pendingImageUri = null,
            pendingImageBase64 = null
        ) }

        viewModelScope.launch {
            var convId = _uiState.value.currentConversationId
            if (convId == null && settingsManager.saveHistory.value) {
                convId = java.util.UUID.randomUUID().toString()
                val title = if (text.isNotBlank()) text.take(30) else "Image conversation"
                repository.insertConversation(ConversationEntity(id = convId, title = title))
                _uiState.update { it.copy(currentConversationId = convId) }
                loadConversation(convId)
            }

            val userMsg = ChatMessageEntity(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = convId ?: "",
                text = displayMessage,
                isUser = true
            )

            if (settingsManager.saveHistory.value) {
                repository.insertMessage(userMsg)
            } else {
                _uiState.update { it.copy(messages = it.messages + userMsg) }
            }

            val parts = mutableListOf<Part>()
            if (text.isNotBlank()) parts.add(Part(text = text))
            if (imageBase64 != null) {
                parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)))
            }
            
            messageHistoryForApi.add(Content(parts = parts))

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("API Key is missing or invalid. Please configure it in Secrets.")
                }

                val request = GenerateContentRequest(
                    contents = messageHistoryForApi.toList(),
                    systemInstruction = Content(parts = listOf(Part(text = "You are Usman AI, a helpful, polite, and intelligent AI assistant.")))
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I'm sorry, I couldn't generate a response."
                
                messageHistoryForApi.add(Content(parts = listOf(Part(text = responseText))))

                val aiMsg = ChatMessageEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    conversationId = convId ?: "",
                    text = responseText, 
                    isUser = false
                )
                
                if (settingsManager.saveHistory.value) {
                    repository.insertMessage(aiMsg)
                } else {
                    _uiState.update { it.copy(messages = it.messages + aiMsg) }
                }
                
                if (settingsManager.voiceResponse.value) {
                    _speakEvent.emit(responseText)
                }

            } catch (e: Exception) {
                val errorMsg = ChatMessageEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    conversationId = convId ?: "",
                    text = "Error: ${e.localizedMessage ?: "Unknown error"}",
                    isUser = false,
                    isError = true
                )
                if (settingsManager.saveHistory.value) {
                    repository.insertMessage(errorMsg)
                } else {
                    _uiState.update { it.copy(messages = it.messages + errorMsg) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
