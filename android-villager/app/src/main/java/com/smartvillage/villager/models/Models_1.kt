package com.smartvillage.villager.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("mobileNumber") val mobileNumber: String,
    val email: String,
    val username: String,
    val password: String,
    @SerializedName("villageName") val villageName: String
)

data class AuthResponse(
    val token: String,
    val role: String,
    val userId: Int,
    val name: String,
    val village: String?
)

data class Complaint(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val imageUrl: String?,
    val location: String?,
    val villageName: String?,
    val createdAt: String,
    val updatedAt: String
)

data class FeedbackRequest(
    val complaintId: Int,
    val rating: Int,
    val comment: String
)

data class FcmTokenRequest(
    val fcmToken: String
)

data class MessageResponse(
    val message: String
)
