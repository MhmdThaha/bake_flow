package com.bakeflow.app

import android.app.Application
import com.bakeflow.app.common.AppContainer
import com.google.firebase.FirebaseApp

/**
 * Application entry point. Initializes Firebase SDK and the app dependency container.
 */
class BakeFlowApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        appContainer = AppContainer(this)
    }
}
