package com.fouwaz.studypal

import android.app.Application
import com.fouwaz.studypal.data.local.VoiceStreamDatabase

class VoiceStreamApplication : Application() {

    lateinit var database: VoiceStreamDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = VoiceStreamDatabase.getDatabase(this)
    }
}
