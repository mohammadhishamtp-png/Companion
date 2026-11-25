package com.adhdcompanion.focus

import android.app.Application
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.notification.NotificationHelper

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
    }
}
