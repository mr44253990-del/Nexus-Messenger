package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val CHANNEL_MESSAGES = "chat_messages_channel"
        const val CHANNEL_GROUPS = "group_messages_channel"
        const val CHANNEL_SOCIAL = "social_notifications_channel"
        const val CHANNEL_REMINDERS = "reminder_notifications_channel"
        private val notificationIdCounter = AtomicInteger(1000)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            serviceScope.launch { FirebaseManager.updateFcmToken(uid, token) }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "EB Chat"
        val messageBody = data["message"] ?: remoteMessage.notification?.body ?: "You received a new message"
        val chatId = data["chatId"]
        val senderId = data["senderId"]
        val receiverId = data["receiverId"]
        val notificationType = data["notificationType"] ?: "message"
        val notificationId = data["notificationId"]

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Check if sender is muted locally (skip in-app notification display but still store)
        // We still show the notification as the user might want to see the badge

        val id = notificationIdCounter.getAndIncrement()

        when (notificationType) {
            "group_message" -> sendGroupNotification(id, title, messageBody, chatId, senderId, notificationId)
            "tag", "comment", "reaction", "follow" -> sendSocialNotification(id, title, messageBody, chatId, senderId, notificationId, notificationType)
            else -> sendChatNotification(id, title, messageBody, chatId, senderId, receiverId, notificationId)
        }

        // Schedule reminder notification after 1.5 hours if not read
        if (notificationType == "message" && chatId != null && senderId != null) {
            scheduleReminderNotification(id + 5000, title, messageBody, chatId, senderId, receiverId, notificationId)
        }

        // Save in-app notification
        serviceScope.launch {
            try {
                FirebaseManager.saveNotification(
                    userId = currentUserId,
                    type = notificationType,
                    body = messageBody,
                    referenceId = chatId ?: notificationId ?: "",
                    senderId = senderId ?: ""
                )
            } catch (e: Exception) {}
        }
    }

    private fun sendChatNotification(
        id: Int, title: String, messageBody: String,
        chatId: String?, senderId: String?, receiverId: String?, notificationId: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createMessageChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("openChat", true)
            putExtra("chatId", chatId)
            putExtra("targetUserId", if (senderId == FirebaseAuth.getInstance().currentUser?.uid) receiverId else senderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val replyIntent = Intent(this, ReplyReceiver::class.java).apply {
            putExtra("chatId", chatId); putExtra("senderId", senderId); putExtra("receiverId", receiverId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(this, id, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).setLabel("Reply...").build()
        val replyAction = NotificationCompat.Action.Builder(android.R.drawable.ic_menu_send, "Reply", replyPendingIntent).addRemoteInput(remoteInput).build()

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setContentIntent(pendingIntent)
            .addAction(replyAction)
            .setAutoCancel(true)
            .setGroup(chatId ?: "messages")
            .setGroupSummary(false)

        NotificationManagerCompat.from(this).notify(id, builder.build())

        // Group summary
        val summaryBuilder = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setGroup(chatId ?: "messages")
            .setGroupSummary(true)
            .setAutoCancel(true)
        NotificationManagerCompat.from(this).notify((chatId?.hashCode() ?: 0) % 10000, summaryBuilder.build())
    }

    private fun sendGroupNotification(
        id: Int, title: String, messageBody: String,
        chatId: String?, senderId: String?, notificationId: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createGroupChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("openGroup", true)
            putExtra("groupId", chatId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, CHANNEL_GROUPS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(chatId ?: "groups")
            .setGroupSummary(false)

        NotificationManagerCompat.from(this).notify(id, builder.build())
    }

    private fun sendSocialNotification(
        id: Int, title: String, messageBody: String,
        chatId: String?, senderId: String?, notificationId: String?, type: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createSocialChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("openNotification", true)
            putExtra("notificationType", type)
            putExtra("referenceId", chatId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, CHANNEL_SOCIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("social")
            .setGroupSummary(false)

        NotificationManagerCompat.from(this).notify(id, builder.build())
    }

    private fun scheduleReminderNotification(
        id: Int, title: String, messageBody: String,
        chatId: String?, senderId: String?, receiverId: String?, notificationId: String?
    ) {
        serviceScope.launch {
            delay(90 * 60 * 1000L) // 1.5 hours
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                createReminderChannel(notificationManager)

                val intent = Intent(this@MyFirebaseMessagingService, MainActivity::class.java).apply {
                    putExtra("openChat", true)
                    putExtra("chatId", chatId)
                    putExtra("targetUserId", if (senderId == FirebaseAuth.getInstance().currentUser?.uid) receiverId else senderId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(this@MyFirebaseMessagingService, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

                val builder = NotificationCompat.Builder(this@MyFirebaseMessagingService, CHANNEL_REMINDERS)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Reminder: $title")
                    .setContentText(messageBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("You have an unread message from earlier"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                NotificationManagerCompat.from(this@MyFirebaseMessagingService).notify(id, builder.build())
            } catch (e: Exception) {}
        }
    }

    private fun createMessageChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val channel = NotificationChannel(CHANNEL_MESSAGES, "Chat Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Personal chat message notifications"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createGroupChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val channel = NotificationChannel(CHANNEL_GROUPS, "Group Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Group chat message notifications"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSocialChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_SOCIAL, "Social Notifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tags, comments, reactions, and other social notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createReminderChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Reminder notifications for unread messages"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}