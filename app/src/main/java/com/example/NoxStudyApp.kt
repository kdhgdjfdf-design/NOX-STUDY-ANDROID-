package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.service.BlockerStateStore
import com.example.service.FocusSessionCoordinator
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class NoxStudyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannels(this)
        database = AppDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)

        // Safe recovery: clears orphaned blocking if app crashed, was killed, or device rebooted
        BlockerStateStore.performSafeRecoveryIfNeeded(this, processToken)
        FocusSessionCoordinator.ensurePrefsObserved(this)

        appScope.launch {
            try {
                preferencesRepository.syncBlockerPreferencesToStore()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        val processToken: String = UUID.randomUUID().toString()

        lateinit var instance: NoxStudyApp
            private set
    }
}
