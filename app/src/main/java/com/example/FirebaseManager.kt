package com.example

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val dob: String = "",
    val gender: String = "",
    val profilePicture: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0
)

object FirebaseManager {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val rtdb = FirebaseDatabase.getInstance().reference

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
    }

    fun unblockUser(uid: String, targetId: String) {
        rtdb.child("blocks").child(uid).child(targetId).removeValue()
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

    fun sendGroupMessage(groupId: String, senderId: String, text: String, voiceUrl: String? = null) {
        val ref = firestore.collection("groups").document(groupId).collection("messages").document()
        val msg = Message(messageId = ref.id, senderId = senderId, text = text, voiceUrl = voiceUrl)
        ref.set(msg)
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
