package com.example.biodontos

import android.app.Application
import com.example.biodontos.core.di.AppContainer
import com.example.biodontos.core.di.DefaultAppContainer

class BiodontosApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
