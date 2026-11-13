package com.smartvillage.villager.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsManager {

    private const val PREF_NAME = "smart_village_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_VILLAGE = "user_village"
    private const val KEY_MOBILE = "user_mobile"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUser(userId: Int, name: String, role: String, village: String?) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_VILLAGE, village ?: "")
            .apply()
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getVillage(): String? {
        return prefs.getString(KEY_VILLAGE, null)
    }

    fun getUserRole(): String? {
        return prefs.getString(KEY_USER_ROLE, null)
    }

    fun saveMobile(mobile: String) {
        prefs.edit().putString(KEY_MOBILE, mobile).apply()
    }

    fun getMobile(): String? {
        return prefs.getString(KEY_MOBILE, null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
