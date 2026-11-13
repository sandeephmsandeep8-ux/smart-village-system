package com.smartvillage.villager.network

import com.smartvillage.villager.models.AuthResponse
import com.smartvillage.villager.models.Complaint
import com.smartvillage.villager.models.FcmTokenRequest
import com.smartvillage.villager.models.FeedbackRequest
import com.smartvillage.villager.models.LoginRequest
import com.smartvillage.villager.models.MessageResponse
import com.smartvillage.villager.models.RegisterRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("villager/complaints")
    suspend fun getComplaints(): Response<List<Complaint>>

    @Multipart
    @POST("villager/complaints")
    suspend fun submitComplaint(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("location") location: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<Complaint>

    @GET("villager/complaints/{id}")
    suspend fun getComplaintById(@Path("id") id: Int): Response<Complaint>

    @POST("villager/feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest): Response<MessageResponse>

    @POST("auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<MessageResponse>
}
