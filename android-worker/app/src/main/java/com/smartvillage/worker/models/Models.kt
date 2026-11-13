package com.smartvillage.worker.models

import com.google.gson.annotations.SerializedName

// ─── Auth ───────────────────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("role") val role: String,
    @SerializedName("userId") val userId: Int,
    @SerializedName("name") val name: String
)

// ─── Dashboard ───────────────────────────────────────────────────────────────

data class WorkerDashboard(
    @SerializedName("assignedCount") val assignedCount: Int,
    @SerializedName("inProgressCount") val inProgressCount: Int,
    @SerializedName("completedCount") val completedCount: Int
)

// ─── Assignment ──────────────────────────────────────────────────────────────

data class Assignment(
    @SerializedName("id") val id: Int,
    @SerializedName("complaintId") val complaintId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("status") val status: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("villageName") val villageName: String?,
    @SerializedName("assignedAt") val assignedAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class AssignmentDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("complaintId") val complaintId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("status") val status: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("villageName") val villageName: String?,
    @SerializedName("reportedBy") val reportedBy: String?,
    @SerializedName("assignedAt") val assignedAt: String,
    @SerializedName("completionPhotoUrl") val completionPhotoUrl: String?,
    @SerializedName("completionRemarks") val completionRemarks: String?
)

// ─── Status Update ───────────────────────────────────────────────────────────

data class StatusUpdateRequest(
    @SerializedName("status") val status: String,
    @SerializedName("remarks") val remarks: String?
)

// ─── FCM ─────────────────────────────────────────────────────────────────────

data class FcmTokenRequest(
    @SerializedName("fcmToken") val fcmToken: String
)

// ─── Generic ─────────────────────────────────────────────────────────────────

data class MessageResponse(
    @SerializedName("message") val message: String
)
