package com.smartvillage.worker

import android.app.Application
import com.smartvillage.worker.network.RetrofitClient
import com.smartvillage.worker.utils.SharedPrefsManager

class SmartVillageWorkerApp : Application() {

    lateinit var prefsManager: SharedPrefsManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefsManager = SharedPrefsManager(this)
        RetrofitClient.init(prefsManager)
    }
}
