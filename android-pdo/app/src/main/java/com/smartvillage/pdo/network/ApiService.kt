package com.smartvillage.pdo.network

import com.smartvillage.pdo.models.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @GET("pdo/dashboard")
    fun getDashboard(): Call<PdoDashboard>

    @GET("pdo/complaints")
    fun getComplaints(): Call<List<Complaint>>

    @GET("pdo/complaints/{id}")
    fun getComplaintDetail(@Path("id") id: Int): Call<ComplaintDetail>

    @PUT("pdo/complaints/{id}/accept")
    fun acceptComplaint(@Path("id") id: Int): Call<MessageResponse>

    @PUT("pdo/complaints/{id}/reject")
    fun rejectComplaint(
        @Path("id") id: Int,
        @Body request: RejectRequest
    ): Call<MessageResponse>

    @POST("pdo/complaints/{id}/assign")
    fun assignComplaint(
        @Path("id") id: Int,
        @Body request: AssignRequest
    ): Call<MessageResponse>

    @PUT("pdo/complaints/{id}/resolve")
    fun resolveComplaint(@Path("id") id: Int): Call<MessageResponse>

    @GET("pdo/workers")
    fun getWorkers(): Call<List<Worker>>

    @POST("auth/fcm-token")
    fun updateFcmToken(@Body request: FcmTokenRequest): Call<MessageResponse>
}
