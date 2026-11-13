package com.smartvillage.worker.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsManager(context: Context) {

    companion object {
        private const val PREF_NAME = "SmartVillageWorkerPrefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_VILLAGE = "user_village"
        private const val KEY_MOBILE = "user_mobile"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ─── Token ───────────────────────────────────────────────────────────────

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    // ─── User Info ───────────────────────────────────────────────────────────

    fun saveUserId(id: Int) {
        prefs.edit().putInt(KEY_USER_ID, id).apply()
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun saveName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun getName(): String? = prefs.getString(KEY_NAME, null)

    fun saveRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun saveVillage(village: String) {
        prefs.edit().putString(KEY_VILLAGE, village).apply()
    }

    fun getVillage(): String? = prefs.getString(KEY_VILLAGE, null)

    fun saveMobile(mobile: String) {
        prefs.edit().putString(KEY_MOBILE, mobile).apply()
    }

    fun getMobile(): String? = prefs.getString(KEY_MOBILE, null)

    // ─── FCM Token ───────────────────────────────────────────────────────────

    fun saveFcmToken(fcmToken: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, fcmToken).apply()
    }

    fun getFcmToken(): String? = prefs.getString(KEY_FCM_TOKEN, null)

    // ─── Bulk Save (after login) ──────────────────────────────────────────────

    fun saveLoginData(token: String, userId: Int, name: String, role: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .putString(KEY_ROLE, role)
            .apply()
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
