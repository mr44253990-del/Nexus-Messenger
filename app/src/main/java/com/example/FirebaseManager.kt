package com.example

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    val dob: String = "",
    val gender: String = "",
    val profilePicture: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0,
    val fcmToken: String = "",
    val mutedUsers: List<String> = emptyList(),
    val blockedUsers: List<String> = emptyList(),
    val themeName: String = "Pink Glass"
)

object FirebaseManager {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val rtdb = FirebaseDatabase.getInstance().reference

    lateinit var context: Context

    private suspend fun sendFcmNotification(to: String, title: String, messageBody: String, chatId: String?, senderId: String?, receiverId: String?) {
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
            }
        )
    }



    data class SocialPost(
        val postId: String = "",
        val authorId: String = "",
        val title: String = "",
        val body: String = "",
        val tags: List<String> = emptyList(),
        val mediaUrl: String = "",
        val mediaType: String = "image",
        val isPrivate: Boolean = false,
        val views: Long = 0,
        val reactions: Map<String, String> = emptyMap(),
        val createdAt: Long = System.currentTimeMillis()
    )

    data class ActivityNotification(
        val notificationId: String = "",
        val userId: String = "",
        val actorId: String = "",
        val type: String = "",
        val title: String = "",
        val body: String = "",
        val targetId: String = "",
        val read: Boolean = false,
        val createdAt: Long = System.currentTimeMillis()
    )

    fun getSocialPosts(): Flow<List<SocialPost>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("isPrivate", false)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(SocialPost::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getNotifications(uid: String): Flow<List<ActivityNotification>> = callbackFlow {
        val listener = firestore.collection("users").document(uid).collection("notifications")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(ActivityNotification::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun createPost(authorId: String, title: String, body: String, tags: List<String>, mediaUrl: String, mediaType: String) {
        val ref = firestore.collection("posts").document()
        ref.set(SocialPost(ref.id, authorId, title, body, tags, mediaUrl, mediaType))
    }

    fun reactToPost(postId: String, uid: String, emoji: String) {
        firestore.collection("posts").document(postId).update("reactions.$uid", emoji)
    }

    fun incrementPostView(postId: String) {
        firestore.collection("posts").document(postId).update("views", com.google.firebase.firestore.FieldValue.increment(1))
    }

    fun setPostPrivate(postId: String, isPrivate: Boolean) {
        firestore.collection("posts").document(postId).update("isPrivate", isPrivate)
    }

    fun deletePost(postId: String) {
        firestore.collection("posts").document(postId).delete()
    }

    fun getAllUsersFlow(): Flow<List<UserProfile>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                    trySend(users)
                }
            }
        awaitClose { listenerRegistration.remove() }
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

    suspend fun createUserProfile(uid: String, name: String, dob: String, gender: String) {
        val username = name.lowercase().replace(" ", "") + "_" + (1000..9999).random()
        val userProfile = UserProfile(
            uid = uid,
            name = name,
            username = username,
            dob = dob,
            gender = gender
        )
        firestore.collection("users").document(uid).set(userProfile).await()
    }

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
        val repliedToText: String? = null
    )

    data class Story(
        val storyId: String = "",
        val userId: String = "",
        val imageUrl: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + 43200000 // 12 hours
    )

    fun getStories(): Flow<List<Story>> = callbackFlow {
        val now = System.currentTimeMillis()
        val listener = firestore.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val stories = snapshot.documents.mapNotNull { it.toObject(Story::class.java) }
                    trySend(stories)
                }
            }
        awaitClose { listener.remove() }
    }

    fun uploadStory(userId: String, imageUrl: String) {
        val ref = firestore.collection("stories").document()
        val story = Story(storyId = ref.id, userId = userId, imageUrl = imageUrl)
        ref.set(story)
    }

    fun deleteMessage(chatId: String, messageId: String) {
        firestore.collection("chats").document(chatId).collection("messages").document(messageId)
            .update("deleted", true, "text", "Message deleted")
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
        firestore.collection("chats").document(chatId).collection("messages").document(messageId)
            .update("text", newText, "edited", true)
    }

    fun blockUser(uid: String, targetId: String) {
        rtdb.child("blocks").child(uid).child(targetId).setValue(true)
        firestore.collection("users").document(uid).update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(targetId))
    }

    fun unblockUser(uid: String, targetId: String) {
        rtdb.child("blocks").child(uid).child(targetId).removeValue()
        firestore.collection("users").document(uid).update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayRemove(targetId))
    }

    fun muteUser(uid: String, targetId: String) {
        firestore.collection("users").document(uid).update("mutedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(targetId))
    }

    fun unmuteUser(uid: String, targetId: String) {
        firestore.collection("users").document(uid).update("mutedUsers", com.google.firebase.firestore.FieldValue.arrayRemove(targetId))
    }

    fun updateProfile(uid: String, name: String, profilePicture: String, themeName: String) {
        firestore.collection("users").document(uid).update(mapOf("name" to name, "profilePicture" to profilePicture, "themeName" to themeName))
    }

    fun isBlocked(uid: String, targetId: String): Flow<Boolean> = callbackFlow {
        val ref = rtdb.child("blocks").child(uid).child(targetId)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                trySend(snapshot.exists())
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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
        repliedToText: String? = null
    ) {
        val messageRef = firestore.collection("chats").document(chatId).collection("messages").document()
        val message = Message(
            messageId = messageRef.id,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            voiceUrl = voiceUrl,
            imageUrl = imageUrl,
            repliedToId = repliedToId,
            repliedToText = repliedToText
        )
        messageRef.set(message)

        // Fetch receiver's token and send notification
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
                            else -> text
                        },
                        chatId = chatId,
                        senderId = senderId,
                        receiverId = receiverId
                    )
                }
            } catch (e: Exception) {
                Log.e("FCM", "Failed to send message notification: ${e.message}")
            }
        }
    }

    fun setTypingStatus(uid: String, receiverId: String, isTyping: Boolean) {
        rtdb.child("typing").child(uid).child(receiverId).setValue(isTyping)
    }

    fun getTypingStatus(uid: String, receiverId: String): Flow<Boolean> = callbackFlow {
        val ref = rtdb.child("typing").child(uid).child(receiverId)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) ?: false)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
    
    fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    data class Group(
        val groupId: String = "",
        val name: String = "",
        val description: String = "",
        val adminId: String = "",
        val members: List<String> = emptyList(),
        val createdAt: Long = System.currentTimeMillis()
    )

    fun createGroup(name: String, description: String, adminId: String) {
        val groupRef = firestore.collection("groups").document()
        val group = Group(
            groupId = groupRef.id,
            name = name,
            description = description,
            adminId = adminId,
            members = listOf(adminId)
        )
        groupRef.set(group)
    }

    fun getGroups(): Flow<List<Group>> = callbackFlow {
        val listener = firestore.collection("groups")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val groups = snapshot.documents.mapNotNull { it.toObject(Group::class.java) }
                    trySend(groups)
                }
            }
        awaitClose { listener.remove() }
    }

    fun addMemberToGroup(groupId: String, userId: String) {
        firestore.collection("groups").document(groupId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
    }

    fun getGroupMessages(groupId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = firestore.collection("groups")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    trySend(messages)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun sendGroupMessage(groupId: String, senderId: String, text: String, voiceUrl: String? = null, imageUrl: String? = null) {
        val ref = firestore.collection("groups").document(groupId).collection("messages").document()
        val msg = Message(messageId = ref.id, senderId = senderId, text = text, voiceUrl = voiceUrl, imageUrl = imageUrl)
        ref.set(msg)

        // Send notification to group topic
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
                    chatId = groupId,
                    senderId = senderId,
                    receiverId = null
                )
            } catch (e: Exception) {
                Log.e("FCM", "Failed to send group notification: ${e.message}")
            }
        }
    }
    fun getCurrentUserProfile(): Flow<UserProfile?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            return@callbackFlow
        }
        val listenerRegistration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val user = snapshot.toObject(UserProfile::class.java)
                    trySend(user)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun setPresence(uid: String) {
        val userStatusDatabaseRef = rtdb.child("status").child(uid)
        
        val isOfflineForDatabase = mapOf(
            "online" to false,
            "lastSeen" to ServerValue.TIMESTAMP
        )
        
        val isOnlineForDatabase = mapOf(
            "online" to true,
            "lastSeen" to ServerValue.TIMESTAMP
        )

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
}
