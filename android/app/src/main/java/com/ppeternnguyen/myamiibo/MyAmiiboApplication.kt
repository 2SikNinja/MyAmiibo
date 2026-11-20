package com.ppeternnguyen.myamiibo

import android.app.Application
import com.ppeternnguyen.myamiibo.data.AppContainer

class MyAmiiboApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
