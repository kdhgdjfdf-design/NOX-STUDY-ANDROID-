package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.service.NotificationHelper

class NoxStudyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannels(this)
        database = AppDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)
    }

    companion object {
        lateinit var instance: NoxStudyApp
            private set
    }
}
