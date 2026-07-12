package com.example

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Context

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val dob: String = "",
    val gender: String = "",
    val profilePicture: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0,
    val fcmToken: String = "",
    val postCount: Int = 0,
    val reactionCount: Int = 0
)

object FirebaseManager {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val rtdb = FirebaseDatabase.getInstance().reference

    lateinit var context: Context

    private suspend fun sendFcmNotification(
        to: String,
        title: String,
        messageBody: String,
        chatId: String? = null,
        senderId: String? = null,
        receiverId: String? = null,
        notificationType: String = "message",
        notificationId: String = System.currentTimeMillis().toString()
    ) {
        val isTopic = to.startsWith("/topics/")
        val topic = if (isTopic) to.removePrefix("/topics/") else null
        val token = if (isTopic) null else to

        FcmHttpV1Manager.sendNotification(
            context = context,
            token = token,
            topic = topic,
            title = title,
            body = messageBody,
            data = mutableMapOf<String, String>().apply {
                if (chatId != null) put("chatId", chatId)
                if (senderId != null) put("senderId", senderId)
                if (receiverId != null) put("receiverId", receiverId)
                put("notificationType", notificationType)
                put("notificationId", notificationId)
            }
        )
    }

    fun getAllUsersFlow(): Flow<List<UserProfile>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                    trySend(users)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) { trySend(snapshot.toObject(UserProfile::class.java)) }
            }
        awaitClose { listener.remove() }
    }

    fun getUserOnlineStatus(uid: String): Flow<Pair<Boolean, Long>> = callbackFlow {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val online = snapshot.child("online").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                trySend(Pair(online, lastSeen))
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(error.toException())
            }
        }
        val ref = rtdb.child("status").child(uid)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        firestore.collection("users").document(uid).update("fcmToken", token).await()
    }

    suspend fun createUserProfile(uid: String, name: String, dob: String, gender: String, profilePicture: String = "") {
        val username = name.lowercase().replace(" ", "") + "_" + (1000..9999).random()
        val userProfile = UserProfile(uid = uid, name = name, username = username, dob = dob, gender = gender, profilePicture = profilePicture)
        firestore.collection("users").document(uid).set(userProfile).await()
    }

    suspend fun updateUserProfile(uid: String, name: String?, bio: String?, profilePicture: String?) {
        val updates = mutableMapOf<String, Any>()
        if (name != null) updates["name"] = name
        if (bio != null) updates["bio"] = bio
        if (profilePicture != null) updates["profilePicture"] = profilePicture
        if (updates.isNotEmpty()) {
            firestore.collection("users").document(uid).update(updates).await()
        }
    }

    // ===== MESSAGES =====
    data class Message(
        val messageId: String = "",
        val senderId: String = "",
        val receiverId: String = "",
        val text: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val edited: Boolean = false,
        val deleted: Boolean = false,
        val voiceUrl: String? = null,
        val imageUrl: String? = null,
        val repliedToId: String? = null,
        val repliedToText: String? = null,
        val forwardedFrom: String? = null,
        val emojiReaction: String? = null
    )

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = firestore.collection("chats")
            .document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    trySend(messages)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        text: String,
        voiceUrl: String? = null,
        imageUrl: String? = null,
        repliedToId: String? = null,
        repliedToText: String? = null,
        forwardedFrom: String? = null
    ) {
        val messageRef = firestore.collection("chats").document(chatId).collection("messages").document()
        val message = Message(
            messageId = messageRef.id, senderId = senderId, receiverId = receiverId,
            text = text, voiceUrl = voiceUrl, imageUrl = imageUrl,
            repliedToId = repliedToId, repliedToText = repliedToText,
            forwardedFrom = forwardedFrom
        )
        messageRef.set(message)

        // Update chat metadata for last message
        val chatMeta = mapOf(
            "lastMessage" to (if (text.isNotEmpty()) text else if (voiceUrl != null) "Voice message" else "Photo"),
            "lastMessageTime" to System.currentTimeMillis(),
            "lastMessageSender" to senderId,
            "participants" to listOf(senderId, receiverId)
        )
        firestore.collection("chats").document(chatId).set(chatMeta, com.google.firebase.firestore.SetOptions.merge())

        // Update unread count
        firestore.collection("chats").document(chatId)
            .collection("unread").document(receiverId)
            .set(mapOf("count" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val receiverProfile = firestore.collection("users").document(receiverId).get().await().toObject(UserProfile::class.java)
                val token = receiverProfile?.fcmToken
                if (!token.isNullOrEmpty()) {
                    val senderProfile = firestore.collection("users").document(senderId).get().await().toObject(UserProfile::class.java)
                    sendFcmNotification(
                        to = token,
                        title = senderProfile?.name ?: "New Message",
                        messageBody = when {
                            !voiceUrl.isNullOrEmpty() -> "Sent a voice message"
                            !imageUrl.isNullOrEmpty() -> "Sent a photo"
                            forwardedFrom != null -> "Forwarded: $text"
                            else -> text
                        },
                        chatId = chatId, senderId = senderId, receiverId = receiverId
                    )
                }
            } catch (e: Exception) { Log.e("FCM", "Failed to send message notification: ${e.message}") }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        firestore.collection("chats").document(chatId).collection("messages").document(messageId)
            .update("deleted", true, "text", "Message deleted")
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
        firestore.collection("chats").document(chatId).collection("messages").document(messageId)
            .update("text", newText, "edited", true)
    }

    fun addEmojiReaction(chatId: String, messageId: String, emoji: String) {
        firestore.collection("chats").document(chatId).collection("messages").document(messageId)
            .update("emojiReaction", emoji)
    }

    fun forwardMessage(originalMessage: Message, toChatId: String, senderId: String, receiverId: String) {
        sendMessage(
            chatId = toChatId, senderId = senderId, receiverId = receiverId,
            text = originalMessage.text,
            voiceUrl = originalMessage.voiceUrl,
            imageUrl = originalMessage.imageUrl,
            forwardedFrom = originalMessage.senderId
        )
    }

    fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    // Chat list with last message
    fun getUserChats(userId: String): Flow<List<ChatListItem>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        val lastMsg = doc.getString("lastMessage") ?: ""
                        val lastTime = doc.getLong("lastMessageTime") ?: 0L
                        val lastSender = doc.getString("lastMessageSender") ?: ""
                        val participants = doc.get("participants") as? List<*> ?: emptyList<Any>()
                        val otherUserId = (participants - userId).firstOrNull() as? String ?: ""
                        ChatListItem(chatId = doc.id, otherUserId = otherUserId, lastMessage = lastMsg, lastMessageTime = lastTime, lastMessageSender = lastSender)
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    data class ChatListItem(
        val chatId: String = "",
        val otherUserId: String = "",
        val lastMessage: String = "",
        val lastMessageTime: Long = 0L,
        val lastMessageSender: String = ""
    )

    fun getUnreadCount(chatId: String, userId: String): Flow<Int> = callbackFlow {
        val ref = firestore.collection("chats").document(chatId).collection("unread").document(userId)
        val listener = ref.addSnapshotListener { snapshot, _ ->
            val count = snapshot?.getLong("count")?.toInt() ?: 0
            trySend(count)
        }
        awaitClose { listener.remove() }
    }

    fun clearUnread(chatId: String, userId: String) {
        firestore.collection("chats").document(chatId).collection("unread").document(userId).delete()
    }

    // ===== TYPING =====
    fun setTypingStatus(uid: String, receiverId: String, isTyping: Boolean) {
        rtdb.child("typing").child(uid).child(receiverId).setValue(isTyping)
    }

    fun getTypingStatus(uid: String, receiverId: String): Flow<Boolean> = callbackFlow {
        val ref = rtdb.child("typing").child(uid).child(receiverId)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) ?: false)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ===== BLOCK / UNBLOCK =====
    fun blockUser(uid: String, targetId: String) {
        rtdb.child("blocks").child(uid).child(targetId).setValue(true)
    }

    fun unblockUser(uid: String, targetId: String) {
        rtdb.child("blocks").child(uid).child(targetId).removeValue()
    }

    fun isBlocked(uid: String, targetId: String): Flow<Boolean> = callbackFlow {
        val ref = rtdb.child("blocks").child(uid).child(targetId)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) { trySend(snapshot.exists()) }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getBlockedUsers(uid: String): Flow<List<String>> = callbackFlow {
        val ref = rtdb.child("blocks").child(uid)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val blocked = snapshot.children.map { it.key ?: "" }
                trySend(blocked)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ===== STORIES (with video, reactions, 12hr auto-delete) =====
    data class Story(
        val storyId: String = "",
        val userId: String = "",
        val mediaUrl: String = "",
        val mediaType: String = "image",
        val timestamp: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + 43200000,
        val reactions: Map<String, String> = emptyMap(),
        val isPrivate: Boolean = false
    )

    fun getStories(): Flow<List<Story>> = callbackFlow {
        val now = System.currentTimeMillis()
        val listener = firestore.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val stories = snapshot.documents.mapNotNull { it.toObject(Story::class.java) }
                    trySend(stories)
                }
            }
        awaitClose { listener.remove() }
    }

    fun uploadStory(userId: String, mediaUrl: String, mediaType: String = "image") {
        val ref = firestore.collection("stories").document()
        val story = Story(storyId = ref.id, userId = userId, mediaUrl = mediaUrl, mediaType = mediaType)
        ref.set(story)
    }

    fun deleteStory(storyId: String) {
        firestore.collection("stories").document(storyId).delete()
    }

    fun addStoryReaction(storyId: String, userId: String, emoji: String) {
        firestore.collection("stories").document(storyId)
            .update("reactions.$userId", emoji)
    }

    fun getUserStories(userId: String): Flow<List<Story>> = callbackFlow {
        val now = System.currentTimeMillis()
        val listener = firestore.collection("stories")
            .whereEqualTo("userId", userId)
            .whereGreaterThan("expiresAt", now)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val stories = snapshot.documents.mapNotNull { it.toObject(Story::class.java) }
                    trySend(stories)
                }
            }
        awaitClose { listener.remove() }
    }

    fun deleteAllUserStories(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = firestore.collection("stories")
                    .whereEqualTo("userId", userId).get().await()
                for (doc in snapshot.documents) { doc.reference.delete() }
            } catch (e: Exception) { Log.e("STORY", "Failed to delete stories: ${e.message}") }
        }
    }

    // ===== POSTS (Social Feed) =====
    data class Post(
        val postId: String = "",
        val userId: String = "",
        val title: String = "",
        val tags: List<String> = emptyList(),
        val taggedUsers: List<String> = emptyList(),
        val imageUrl: String? = null,
        val videoUrl: String? = null,
        val caption: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val reactions: Map<String, String> = emptyMap(),
        val commentCount: Long = 0,
        val viewCount: Long = 0,
        val isPrivate: Boolean = false
    )

    data class Comment(
        val commentId: String = "",
        val postId: String = "",
        val userId: String = "",
        val text: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val userName: String = ""
    )

    fun getPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                        .filter { !it.isPrivate }
                    trySend(posts)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                    trySend(posts)
                }
            }
        awaitClose { listener.remove() }
    }

    fun createPost(userId: String, title: String, tags: List<String>, taggedUsers: List<String>, imageUrl: String?, videoUrl: String?, caption: String) {
        val ref = firestore.collection("posts").document()
        val post = Post(postId = ref.id, userId = userId, title = title, tags = tags, taggedUsers = taggedUsers, imageUrl = imageUrl, videoUrl = videoUrl, caption = caption)
        ref.set(post)

        // Update user post count
        firestore.collection("users").document(userId)
            .update("postCount", FieldValue.increment(1))

        // Notify tagged users
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val senderProfile = firestore.collection("users").document(userId).get().await().toObject(UserProfile::class.java)
                for (taggedUid in taggedUsers) {
                    val taggedProfile = firestore.collection("users").document(taggedUid).get().await().toObject(UserProfile::class.java)
                    val token = taggedProfile?.fcmToken
                    if (!token.isNullOrEmpty()) {
                        sendFcmNotification(
                            to = token,
                            title = "You were tagged in a post",
                            messageBody = "${senderProfile?.name} tagged you in: $title",
                            notificationType = "tag",
                            notificationId = ref.id
                        )
                    }
                    // Save in-app notification
                    saveNotification(taggedUid, "tag", "${senderProfile?.name} tagged you in a post", ref.id, userId)
                }
            } catch (e: Exception) { Log.e("POST", "Failed to notify tagged users: ${e.message}") }
        }
    }

    fun deletePost(postId: String, userId: String) {
        firestore.collection("posts").document(postId).delete()
        // Delete associated stories if video
        deleteAllUserStories(userId)
        firestore.collection("users").document(userId)
            .update("postCount", FieldValue.increment(-1))
    }

    fun togglePostPrivacy(postId: String, isPrivate: Boolean) {
        firestore.collection("posts").document(postId).update("isPrivate", isPrivate)
    }

    fun reactToPost(postId: String, userId: String, emoji: String) {
        firestore.collection("posts").document(postId)
            .update("reactions.$userId", emoji)
    }

    fun removePostReaction(postId: String, userId: String) {
        val updates = mapOf("reactions.$userId" to FieldValue.delete())
        firestore.collection("posts").document(postId).update(updates)
    }

    fun incrementViewCount(postId: String) {
        firestore.collection("posts").document(postId)
            .update("viewCount", FieldValue.increment(1))
    }

    fun addComment(postId: String, userId: String, text: String, userName: String) {
        val ref = firestore.collection("posts").document(postId).collection("comments").document()
        val comment = Comment(commentId = ref.id, postId = postId, userId = userId, text = text, timestamp = System.currentTimeMillis(), userName = userName)
        ref.set(comment)
        firestore.collection("posts").document(postId)
            .update("commentCount", FieldValue.increment(1))

        // Notify post owner
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val postSnapshot = firestore.collection("posts").document(postId).get().await()
                val postOwnerId = postSnapshot.getString("userId") ?: ""
                if (postOwnerId != userId) {
                    val commenterProfile = firestore.collection("users").document(userId).get().await().toObject(UserProfile::class.java)
                    saveNotification(postOwnerId, "comment", "${commenterProfile?.name} commented on your post", postId, userId)
                }
            } catch (e: Exception) { Log.e("COMMENT", "Failed: ${e.message}") }
        }
    }

    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { it.toObject(Comment::class.java) }
                    trySend(comments)
                }
            }
        awaitClose { listener.remove() }
    }

    // ===== GROUPS =====
    data class Group(
        val groupId: String = "",
        val name: String = "",
        val description: String = "",
        val adminId: String = "",
        val members: List<String> = emptyList(),
        val groupPicture: String = "",
        val createdAt: Long = System.currentTimeMillis()
    )

    fun createGroup(name: String, description: String, adminId: String, groupPicture: String = "") {
        val groupRef = firestore.collection("groups").document()
        val group = Group(groupId = groupRef.id, name = name, description = description, adminId = adminId, members = listOf(adminId), groupPicture = groupPicture)
        groupRef.set(group)

        // Subscribe admin to group topic
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("group_${groupRef.id}")
            } catch (e: Exception) {}
        }
    }

    fun getGroups(): Flow<List<Group>> = callbackFlow {
        val listener = firestore.collection("groups")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val groups = snapshot.documents.mapNotNull { it.toObject(Group::class.java) }
                    trySend(groups)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getUserGroups(userId: String): Flow<List<Group>> = callbackFlow {
        val listener = firestore.collection("groups")
            .whereArrayContains("members", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val groups = snapshot.documents.mapNotNull { it.toObject(Group::class.java) }
                    trySend(groups)
                }
            }
        awaitClose { listener.remove() }
    }

    fun addMemberToGroup(groupId: String, userId: String) {
        firestore.collection("groups").document(groupId)
            .update("members", FieldValue.arrayUnion(userId))
        // Subscribe to topic
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("group_$groupId")
            } catch (e: Exception) {}
        }
    }

    fun removeMemberFromGroup(groupId: String, userId: String) {
        firestore.collection("groups").document(groupId)
            .update("members", FieldValue.arrayRemove(userId))
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("group_$groupId")
            } catch (e: Exception) {}
        }
    }

    fun deleteGroup(groupId: String) {
        firestore.collection("groups").document(groupId).delete()
    }

    fun getGroupMessages(groupId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = firestore.collection("groups")
            .document(groupId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    trySend(messages)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun sendGroupMessage(groupId: String, senderId: String, text: String, voiceUrl: String? = null, imageUrl: String? = null, repliedToId: String? = null, repliedToText: String? = null) {
        val ref = firestore.collection("groups").document(groupId).collection("messages").document()
        val msg = Message(messageId = ref.id, senderId = senderId, text = text, voiceUrl = voiceUrl, imageUrl = imageUrl, repliedToId = repliedToId, repliedToText = repliedToText)
        ref.set(msg)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val senderProfile = firestore.collection("users").document(senderId).get().await().toObject(UserProfile::class.java)
                sendFcmNotification(
                    to = "/topics/group_$groupId",
                    title = "Group Message",
                    messageBody = "${senderProfile?.name}: " + when {
                        !voiceUrl.isNullOrEmpty() -> "Sent a voice message"
                        !imageUrl.isNullOrEmpty() -> "Sent a photo"
                        else -> text
                    },
                    chatId = groupId, senderId = senderId,
                    notificationType = "group_message"
                )
            } catch (e: Exception) { Log.e("FCM", "Failed to send group notification: ${e.message}") }
        }
    }

    fun deleteGroupMessage(groupId: String, messageId: String) {
        firestore.collection("groups").document(groupId).collection("messages").document(messageId)
            .update("deleted", true, "text", "Message deleted")
    }

    // ===== IN-APP NOTIFICATIONS =====
    data class AppNotification(
        val notificationId: String = "",
        val userId: String = "",
        val type: String = "",
        val title: String = "",
        val body: String = "",
        val referenceId: String = "",
        val senderId: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val isRead: Boolean = false
    )

    fun saveNotification(userId: String, type: String, body: String, referenceId: String, senderId: String) {
        val ref = firestore.collection("notifications").document()
        val notification = AppNotification(
            notificationId = ref.id, userId = userId, type = type,
            title = when (type) {
                "message" -> "New Message"
                "comment" -> "New Comment"
                "tag" -> "You were tagged"
                "reaction" -> "New Reaction"
                "group_message" -> "Group Message"
                "follow" -> "New Follower"
                else -> "Notification"
            },
            body = body, referenceId = referenceId, senderId = senderId
        )
        ref.set(notification)
    }

    fun getUserNotifications(userId: String): Flow<List<AppNotification>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { it.toObject(AppNotification::class.java) }
                    trySend(notifications)
                }
            }
        awaitClose { listener.remove() }
    }

    fun markNotificationRead(notificationId: String) {
        firestore.collection("notifications").document(notificationId).update("isRead", true)
    }

    fun getUnreadNotificationCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { }
    }

    // ===== PRESENCE =====
    fun getCurrentUserProfile(): Flow<UserProfile?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(null); return@callbackFlow }
        val listenerRegistration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) { trySend(snapshot.toObject(UserProfile::class.java)) }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun setPresence(uid: String) {
        val userStatusDatabaseRef = rtdb.child("status").child(uid)
        val isOfflineForDatabase = mapOf("online" to false, "lastSeen" to ServerValue.TIMESTAMP)
        val isOnlineForDatabase = mapOf("online" to true, "lastSeen" to ServerValue.TIMESTAMP)
        rtdb.child(".info/connected").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    userStatusDatabaseRef.onDisconnect().setValue(isOfflineForDatabase).addOnCompleteListener {
                        userStatusDatabaseRef.setValue(isOnlineForDatabase)
                    }
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    fun searchUsers(query: String): Flow<List<UserProfile>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                        .filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.username.contains(query, ignoreCase = true) ||
                            it.uid == query
                        }
                    trySend(users)
                }
            }
        awaitClose { }
    }
}