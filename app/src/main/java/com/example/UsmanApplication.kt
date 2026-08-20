package com.example

import android.app.Application
import androidx.room.Room

class UsmanApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "usman_ai_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    val repository by lazy {
        ChatRepository(database.chatDao())
    }
    
    val settingsManager by lazy {
        SettingsManager(this)
    }
}
