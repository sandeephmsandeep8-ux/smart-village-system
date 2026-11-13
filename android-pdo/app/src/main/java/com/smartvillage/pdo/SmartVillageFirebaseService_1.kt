package com.smartvillage.pdo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartvillage.pdo.models.FcmTokenRequest
import com.smartvillage.pdo.models.MessageResponse
import com.smartvillage.pdo.network.RetrofitClient
import com.smartvillage.pdo.utils.SharedPrefsManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SmartVillageFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "smart_village_pdo_channel"
        private const val CHANNEL_NAME = "Smart Village PDO Notifications"
        private const val CHANNEL_DESC = "Notifications for PDO Officer app"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefsManager = SharedPrefsManager(applicationContext)
        prefsManager.saveFcmToken(token)

        if (prefsManager.isLoggedIn()) {
            RetrofitClient.init(prefsManager)
            val request = FcmTokenRequest(token)
            RetrofitClient.getInstance().updateFcmToken(request)
                .enqueue(object : Callback<MessageResponse> {
                    override fun onResponse(
                        call: Call<MessageResponse>,
                        response: Response<MessageResponse>
                    ) {
                        // Token updated successfully
                    }

                    override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                        // Will retry on next token refresh
                    }
                })
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Smart Village PDO"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "You have a new notification"

        val complaintId = remoteMessage.data["complaintId"]?.toIntOrNull()

        showNotification(title, body, complaintId)
    }

    private fun showNotification(title: String, body: String, complaintId: Int?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build pending intent
        val intent = if (complaintId != null) {
            Intent(this, ComplaintDetailActivity::class.java).apply {
                putExtra("COMPLAINT_ID", complaintId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            complaintId ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
