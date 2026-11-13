package com.smartvillage.pdo.models

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val role: String,
    val userId: Int,
    val name: String
)

data class PdoDashboard(
    val pendingCount: Int,
    val inProgressCount: Int,
    val resolvedCount: Int,
    val totalCount: Int
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
    val reportedBy: String?,
    val createdAt: String
)

data class ComplaintDetail(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val imageUrl: String?,
    val location: String?,
    val villageName: String?,
    val reportedBy: String?,
    val assignedWorker: String?,
    val createdAt: String,
    val updatedAt: String
)

data class RejectRequest(
    val reason: String
)

data class AssignRequest(
    val workerId: Int
)

data class Worker(
    val id: Int,
    val fullName: String,
    val villageName: String?
)

data class FcmTokenRequest(
    val fcmToken: String
)

data class MessageResponse(
    val message: String
)
