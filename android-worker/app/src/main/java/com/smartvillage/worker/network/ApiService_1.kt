package com.smartvillage.worker.network

import com.smartvillage.worker.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Auth ────────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<MessageResponse>

    // ─── Worker Dashboard ────────────────────────────────────────────────────

    @GET("worker/dashboard")
    suspend fun getWorkerDashboard(): Response<WorkerDashboard>

    // ─── Assignments ─────────────────────────────────────────────────────────

    @GET("worker/assignments")
    suspend fun getAssignments(): Response<List<Assignment>>

    @GET("worker/assignments/{id}")
    suspend fun getAssignmentDetail(@Path("id") id: Int): Response<AssignmentDetail>

    @PUT("worker/assignments/{id}/accept")
    suspend fun acceptAssignment(@Path("id") id: Int): Response<MessageResponse>

    @PUT("worker/assignments/{id}/status")
    suspend fun updateAssignmentStatus(
        @Path("id") id: Int,
        @Body request: StatusUpdateRequest
    ): Response<MessageResponse>

    @Multipart
    @POST("worker/assignments/{id}/upload-photo")
    suspend fun uploadCompletionPhoto(
        @Path("id") id: Int,
        @Part photo: MultipartBody.Part
    ): Response<MessageResponse>
}
