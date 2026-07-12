package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "chat_notification_channel"
    private val NOTIFICATION_ID = 202
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            serviceScope.launch {
                FirebaseManager.updateFcmToken(uid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle both data and notification messages
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "New Message"
        val message = remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: "You received a new message"
        
        val chatId = remoteMessage.data["chatId"]
        val senderId = remoteMessage.data["senderId"] // This is the person who sent it
        val receiverId = remoteMessage.data["receiverId"] // This should be our ID

        sendNotification(title, message, chatId, senderId, receiverId)
    }

    private fun sendNotification(title: String, messageBody: String, chatId: String?, senderId: String?, receiverId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        // 1. Intent for the direct reply
        val replyIntent = Intent(this, ReplyReceiver::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("senderId", senderId)
            putExtra("receiverId", receiverId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, 0, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // 2. RemoteInput for the typing box
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel("Write a reply...")
            build()
        }

        // 3. Add reply action to the notification
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, 
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // 4. Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH) 
            .addAction(replyAction) 
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used for chat notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
