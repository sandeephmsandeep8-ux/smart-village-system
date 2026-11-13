package com.smartvillage.pdo.network

import com.smartvillage.pdo.BuildConfig
import com.smartvillage.pdo.utils.SharedPrefsManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var instance: ApiService? = null
    private var prefsManager: SharedPrefsManager? = null

    fun init(manager: SharedPrefsManager) {
        prefsManager = manager
        instance = null
    }

    private fun buildClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = prefsManager?.getToken()
            val request = if (!token.isNullOrEmpty()) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getInstance(): ApiService {
        if (instance == null) {
            instance = Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL + "/")
                .client(buildClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
        return instance!!
    }

    fun resetInstance() {
        instance = null
    }
}
