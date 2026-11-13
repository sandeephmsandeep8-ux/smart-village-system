package com.smartvillage.villager

import android.app.Application
import com.smartvillage.villager.utils.SharedPrefsManager

class SmartVillageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SharedPrefsManager.init(this)
    }
}
