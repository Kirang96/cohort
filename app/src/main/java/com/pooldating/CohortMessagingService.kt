package com.pooldating

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pooldating.ui.home.HomeActivity
import com.pooldating.ui.chat.ChatActivity
import com.pooldating.ui.matches.MatchesActivity

/**
 * FCM Messaging Service for Cohort
 * 
 * CONSTRAINTS (per Iteration 6 spec):
 * - Notifications NEVER modify state
 * - Notifications NEVER trigger logic
 * - On tap → navigate to relevant screen + re-fetch from Firestore
 * - NEVER trust payload for logic decisions
 */
class CohortMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FCM"
        const val CHANNEL_ID = "cohort_notifications"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        
        // Register token with backend
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            registerTokenWithBackend(token)
        } else {
            // Token will be registered when user logs in
            Log.d(TAG, "User not logged in, token will be registered on login")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Message received: ${message.data}")
        
        // Extract notification data
        val type = message.data["type"] ?: "UNKNOWN"
        val entityId = message.data["entity_id"] ?: ""
        val title = message.notification?.title ?: "Cohort"
        val body = message.notification?.body ?: "You have a new notification"
        
        // Show notification
        showNotification(type, entityId, title, body)
    }

    private fun showNotification(type: String, entityId: String, title: String, body: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel (required for Android O+)
        createNotificationChannel(notificationManager)
        
        // Build intent based on notification type
        // NOTE: Intent only navigates - screen will re-fetch data from Firestore
        val intent = when (type) {
            "CHAT_MESSAGE", "CHAT_EXPIRED", "CHAT_EXPIRING", "CHAT_CONTINUED" -> {
                Intent(this, ChatActivity::class.java).apply {
                    putExtra("match_id", entityId)
                }
            }
            "MATCH_READY" -> {
                Intent(this, MatchesActivity::class.java).apply {
                    putExtra("POOL_ID", entityId)
                }
            }
            else -> {
                // Default: go to home
                Intent(this, HomeActivity::class.java)
            }
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cohort Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for matches, chats, and pool updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerTokenWithBackend(token: String) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("registerDeviceToken")
            .call(hashMapOf("fcm_token" to token))
            .addOnSuccessListener {
                Log.d(TAG, "Token registered with backend")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to register token: ${e.message}")
            }
    }
}
