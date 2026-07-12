package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val replyText = remoteInput.getCharSequence(MyFirebaseMessagingService.KEY_TEXT_REPLY).toString()
            
            val chatId = intent.getStringExtra("chatId")
            val senderId = intent.getStringExtra("senderId") // The person we are replying TO
            val receiverId = intent.getStringExtra("receiverId") // Our ID

            if (chatId != null && senderId != null && receiverId != null && replyText.isNotBlank()) {
                // Send reply to Firebase
                FirebaseManager.sendMessage(
                    chatId = chatId,
                    senderId = receiverId, // We are the sender now
                    receiverId = senderId, // Original sender is now the receiver
                    text = replyText
                )
            }

            // Cancel the notification or update it
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(202) // Matches MyFirebaseMessagingService.NOTIFICATION_ID
        }
    }
}
