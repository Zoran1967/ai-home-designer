package com.zoran.aihomedesigner

import android.app.Application

class AiHomeDesignerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
