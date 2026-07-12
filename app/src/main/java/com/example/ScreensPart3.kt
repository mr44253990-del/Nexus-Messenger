package com.example

import android.Manifest
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mute
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.ui.theme.LocalThemeColors
import com.example.ui.theme.NotificationRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.OnlineGreenBright
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// ============================================================
// CHATS SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: return
    val colors = LocalThemeColors.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val chatList by FirebaseManager.getUserChats(currentUserId).collectAsState(initial = emptyList())
    val allUsers by FirebaseManager.getAllUsersFlow().collectAsState(initial = emptyList())
    val stories by FirebaseManager.getStories().collectAsState(initial = emptyList())
    val mutedUsers by PreferenceManager.getMutedUsers(context).collectAsState(initial = emptySet())
    val unreadNotificationCount by FirebaseManager.getUnreadNotificationCount(currentUserId).collectAsState(initial = 0)

    val usersMap = remember(allUsers) { allUsers.associateBy { it.uid } }

    val filteredChatList = remember(chatList, searchQuery, usersMap) {
        if (searchQuery.isBlank()) {
            chatList
        } else {
            chatList.filter { chat ->
                val profile = usersMap[chat.otherUserId]
                profile != null && (
                    profile.name.contains(searchQuery, ignoreCase = true) ||
                    profile.username.contains(searchQuery, ignoreCase = true) ||
                    profile.uid == searchQuery
                    )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name, username or ID...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        )
                    } else {
                        Text("Chats", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearching = !isSearching
                        if (!isSearching) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearching) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (isSearching) "Close search" else "Search"
                        )
                    }
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge {
                                    Text(
                                        if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { /* navigate to notifications */ }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Stories bar
            StoriesBar(stories, allUsers, navController)

            Divider(color = colors.glassBorder, thickness = 0.5.dp)

            // Chat list
            if (filteredChatList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No chats match \"$searchQuery\"" else "No conversations yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredChatList, key = { it.chatId }) { chatItem ->
                        val userProfile = usersMap[chatItem.otherUserId]
                        if (userProfile != null) {
                            val unreadCount by FirebaseManager.getUnreadCount(chatItem.chatId, currentUserId)
                                .collectAsState(initial = 0)
                            val isMuted = mutedUsers.contains(chatItem.otherUserId)
                            val onlineStatus by FirebaseManager.getUserOnlineStatus(chatItem.otherUserId)
                                .collectAsState(initial = Pair(false, 0L))

                            EnhancedChatListItem(
                                user = userProfile,
                                chatItem = chatItem,
                                unreadCount = unreadCount,
                                isMuted = isMuted,
                                isOnline = onlineStatus.first,
                                onClick = {
                                    navController.navigate("chat_detail/${userProfile.uid}/${userProfile.name}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedChatListItem(
    user: UserProfile,
    chatItem: FirebaseManager.ChatListItem,
    unreadCount: Int,
    isMuted: Boolean,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalThemeColors.current
    val isOwnLastMessage = chatItem.lastMessageSender == FirebaseAuth.getInstance().currentUser?.uid

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online indicator
            Box(modifier = Modifier.size(54.dp)) {
                if (user.profilePicture.isNotEmpty()) {
                    AsyncImage(
                        model = user.profilePicture,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                // Online status dot
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(OnlineGreenBright)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            user.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isMuted) {
                            Icon(
                                Icons.Filled.VolumeOff,
                                contentDescription = "Muted",
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(start = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Text(
                        formatTime(chatItem.lastMessageTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        buildString {
                            if (isOwnLastMessage) append("You: ")
                            append(chatItem.lastMessage)
                        },
                        fontSize = 14.sp,
                        color = if (isMuted)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCount > 0 && !isMuted) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(NotificationRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// STORIES BAR
// ============================================================

@Composable
fun StoriesBar(
    stories: List<FirebaseManager.Story>,
    allUsers: List<UserProfile>,
    navController: androidx.navigation.NavHostController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val colors = LocalThemeColors.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && auth.currentUser != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val url = SupabaseManager.uploadFile("stories", "${UUID.randomUUID()}.jpg", bytes)
                        FirebaseManager.uploadStory(auth.currentUser!!.uid, url)
                        Toast.makeText(context, "Story uploaded!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        // Add story button
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(colors.glass.copy(alpha = 0.5f))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AddCircleOutline,
                        contentDescription = "Add Story",
                        modifier = Modifier.size(28.dp),
                        tint = colors.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("My Story", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // User stories grouped by userId
        val userStories = stories.groupBy { it.userId }
        userStories.forEach { (userId, userStoriesList) ->
            val user = allUsers.find { it.uid == userId }
            if (user != null) {
                item {
                    StoryRingItem(user = user)
                }
            }
        }
    }
}

@Composable
fun StoryRingItem(user: UserProfile) {
    val colors = LocalThemeColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(colors.primary, colors.accent)
                    ),
                    shape = CircleShape
                )
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (user.profilePicture.isNotEmpty()) {
                AsyncImage(
                    model = user.profilePicture,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            user.name.split(" ").first(),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// CHAT DETAIL SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: androidx.navigation.NavHostController,
    targetUserId: String,
    targetUserName: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: return
    val colors = LocalThemeColors.current
    val chatId = remember(currentUserId, targetUserId) {
        FirebaseManager.getChatId(currentUserId, targetUserId)
    }

    var messageText by remember { mutableStateOf("") }
    var repliedToMessage by remember { mutableStateOf<FirebaseManager.Message?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var forwardMessage by remember { mutableStateOf<FirebaseManager.Message?>(null) }
    var editingMessage by remember { mutableStateOf<FirebaseManager.Message?>(null) }
    var editText by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

    val messages by FirebaseManager.getMessages(chatId).collectAsState(initial = emptyList())
    val isTargetTyping by FirebaseManager.getTypingStatus(targetUserId, currentUserId)
        .collectAsState(initial = false)
    val status by FirebaseManager.getUserOnlineStatus(targetUserId)
        .collectAsState(initial = Pair(false, 0L))
    val isTargetOnline = status.first
    val lastSeen = status.second
    val isBlockedByMe by FirebaseManager.isBlocked(currentUserId, targetUserId)
        .collectAsState(initial = false)

    val mutedUsers by PreferenceManager.getMutedUsers(context).collectAsState(initial = emptySet())
    val isMuted = mutedUsers.contains(targetUserId)

    // Map for forward-from names
    val allUsers by FirebaseManager.getAllUsersFlow().collectAsState(initial = emptyList())
    val usersMap = remember(allUsers) { allUsers.associateBy { it.uid } }

    val statusText = when {
        isBlockedByMe -> "You blocked this user"
        isTargetTyping -> "typing..."
        isTargetOnline -> "Online"
        lastSeen > 0 -> "Last seen ${formatTime(lastSeen)}"
        else -> "Offline"
    }

    // Clear unread on enter
    LaunchedEffect(chatId) {
        FirebaseManager.clearUnread(chatId, currentUserId)
    }

    // Clear typing on leave
    DisposableEffect(Unit) {
        onDispose {
            FirebaseManager.setTypingStatus(currentUserId, targetUserId, false)
        }
    }

    val recorder = remember { VoiceRecorder(context) }
    val recordAudioPermission = androidx.compose.runtime.rememberCoroutineScope()

    val hasRecordPermission = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasRecordPermission.value = granted }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val url = SupabaseManager.uploadFile("images", "${UUID.randomUUID()}.jpg", bytes)
                        FirebaseManager.sendMessage(
                            chatId, currentUserId, targetUserId, "",
                            imageUrl = url,
                            repliedToId = repliedToMessage?.messageId,
                            repliedToText = repliedToMessage?.text
                        )
                        repliedToMessage = null
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Typing indicator pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "typingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typingAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTargetOnline && !isBlockedByMe) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(OnlineGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(targetUserName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                statusText,
                                fontSize = 12.sp,
                                color = when {
                                    isBlockedByMe -> MaterialTheme.colorScheme.error
                                    isTargetTyping -> OnlineGreen.copy(alpha = pulseAlpha)
                                    isTargetOnline -> OnlineGreen
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isMuted) "Unmute" else "Mute") },
                            onClick = {
                                showMoreMenu = false
                                coroutineScope.launch {
                                    PreferenceManager.toggleMuteUser(context, targetUserId)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    if (isMuted) Icons.Filled.NotificationsOff else Icons.Filled.Notifications,
                                    contentDescription = null
                                )
                            }
                        )
                        if (isBlockedByMe) {
                            DropdownMenuItem(
                                text = { Text("Unblock") },
                                onClick = {
                                    showMoreMenu = false
                                    FirebaseManager.unblockUser(currentUserId, targetUserId)
                                    Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Person, contentDescription = null)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Block") },
                                onClick = {
                                    showMoreMenu = false
                                    FirebaseManager.blockUser(currentUserId, targetUserId)
                                    Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Block, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("View Profile") },
                            onClick = {
                                showMoreMenu = false
                                // Could navigate to profile screen
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            Column {
                // Reply preview bar
                if (repliedToMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = colors.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Replying to",
                                    fontSize = 10.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    repliedToMessage!!.text.take(60),
                                    maxLines = 1,
                                    fontSize = 12.sp,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { repliedToMessage = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Cancel Reply",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Typing indicator
                if (isTargetTyping && !isBlockedByMe) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "typing",
                            fontSize = 12.sp,
                            color = OnlineGreen.copy(alpha = pulseAlpha),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Animated dots
                        repeat(3) { index ->
                            val dotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(400, delayMillis = index * 150),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dot$index"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(1.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(OnlineGreen.copy(alpha = dotAlpha))
                            )
                        }
                    }
                }

                // Input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image picker
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = "Send Image",
                            tint = colors.primary
                        )
                    }

                    // Text field or recording UI
                    if (!isRecording) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = {
                                messageText = it
                                FirebaseManager.setTypingStatus(currentUserId, targetUserId, it.isNotEmpty())
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    if (isBlockedByMe) "Unblock to send messages" else "Message",
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isBlockedByMe,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        // Recording indicator
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Recording pulse
                                val recPulse by infiniteTransition.animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(500),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "recPulse"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = recPulse))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recording...", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Mic / Send / Stop recording button
                    if (isRecording) {
                        // Stop and send recording
                        IconButton(onClick = {
                            try {
                                isRecording = false
                                val file = recorder.stopRecording()
                                if (file != null) {
                                    coroutineScope.launch {
                                        try {
                                            val url = SupabaseManager.uploadFile(
                                                "voices",
                                                "${UUID.randomUUID()}.mp3",
                                                file.readBytes()
                                            )
                                            FirebaseManager.sendMessage(
                                                chatId, currentUserId, targetUserId, "",
                                                voiceUrl = url
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Upload failed: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                isRecording = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice", tint = Color.Red)
                        }
                        // Cancel recording
                        IconButton(onClick = {
                            isRecording = false
                            recorder.cancelRecording()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Cancel Voice", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (messageText.isBlank() && !isBlockedByMe) {
                        // Mic button
                        IconButton(onClick = {
                            if (hasRecordPermission.value) {
                                try {
                                    isRecording = true
                                    recorder.startRecording()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Recorder error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Voice Message", tint = colors.primary)
                        }
                    } else if (messageText.isNotBlank() && !isBlockedByMe) {
                        // Send button
                        FloatingActionButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    FirebaseManager.sendMessage(
                                        chatId,
                                        currentUserId,
                                        targetUserId,
                                        messageText.trim(),
                                        repliedToId = repliedToMessage?.messageId,
                                        repliedToText = repliedToMessage?.text
                                    )
                                    messageText = ""
                                    repliedToMessage = null
                                    FirebaseManager.setTypingStatus(currentUserId, targetUserId, false)
                                }
                            },
                            modifier = Modifier.size(42.dp),
                            containerColor = colors.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        // Message list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.filter { !it.deleted }, key = { it.messageId }) { msg ->
                var swipeOffsetX by remember { mutableFloatStateOf(0f) }

                MessageBubble(
                    msg = msg,
                    isMe = msg.senderId == currentUserId,
                    usersMap = usersMap,
                    colors = colors,
                    onReply = {
                        repliedToMessage = it
                    },
                    onForward = {
                        forwardMessage = it
                        showForwardDialog = true
                    },
                    onReact = { emoji ->
                        FirebaseManager.addEmojiReaction(chatId, msg.messageId, emoji)
                    },
                    onDelete = {
                        FirebaseManager.deleteMessage(chatId, msg.messageId)
                    },
                    onEdit = {
                        editingMessage = it
                        editText = it.text
                        showEditDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = swipeOffsetX.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (swipeOffsetX > 40f) {
                                        // Swipe right = reply
                                        repliedToMessage = msg
                                    }
                                    swipeOffsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val newOffset = swipeOffsetX + (dragAmount / 50f)
                                    swipeOffsetX = newOffset.coerceIn(0f, 80f)
                                }
                            )
                        }
                )
            }
        }
    }

    // Forward dialog
    if (showForwardDialog && forwardMessage != null) {
        ForwardDialog(
            currentUserId = currentUserId,
            allUsers = allUsers,
            onDismiss = {
                showForwardDialog = false
                forwardMessage = null
            },
            onForward = { selectedUserId ->
                val selectedUser = allUsers.find { it.uid == selectedUserId }
                if (selectedUser != null && forwardMessage != null) {
                    val toChatId = FirebaseManager.getChatId(currentUserId, selectedUserId)
                    FirebaseManager.forwardMessage(forwardMessage!!, toChatId, currentUserId, selectedUserId)
                    FirebaseManager.saveNotification(
                        selectedUserId, "message",
                        "Forwarded a message from you",
                        toChatId, currentUserId
                    )
                    Toast.makeText(context, "Message forwarded to ${selectedUser.name}", Toast.LENGTH_SHORT).show()
                }
                showForwardDialog = false
                forwardMessage = null
            }
        )
    }

    // Edit dialog
    if (showEditDialog && editingMessage != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editText.isNotBlank() && editingMessage != null) {
                        FirebaseManager.editMessage(chatId, editingMessage!!.messageId, editText.trim())
                        Toast.makeText(context, "Message edited", Toast.LENGTH_SHORT).show()
                    }
                    showEditDialog = false
                    editingMessage = null
                }) {
                    Text("Save", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    editingMessage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================
// MESSAGE BUBBLE
// ============================================================

@Composable
fun MessageBubble(
    msg: FirebaseManager.Message,
    isMe: Boolean,
    usersMap: Map<String, UserProfile>,
    colors: com.example.ui.theme.ThemeColors,
    onReply: (FirebaseManager.Message) -> Unit,
    onForward: (FirebaseManager.Message) -> Unit,
    onReact: (String) -> Unit,
    onDelete: () -> Unit,
    onEdit: (FirebaseManager.Message) -> Unit,
    modifier: Modifier = Modifier
) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) {
        colors.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    var showMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .padding(start = if (isMe) 48.dp else 0.dp, end = if (!isMe) 48.dp else 0.dp),
        horizontalAlignment = alignment
    ) {
        // Forwarded from label
        if (!msg.forwardedFrom.isNullOrEmpty()) {
            val forwarderName = usersMap[msg.forwardedFrom]?.name ?: "Unknown"
            Text(
                "Forwarded from $forwarderName",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp),
                textAlign = if (isMe) TextAlign.End else TextAlign.Start
            )
        }

        // Reply quote
        if (!msg.repliedToText.isNullOrEmpty()) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .padding(horizontal = 4.dp),
                color = if (isMe) Color.White.copy(alpha = 0.2f) else colors.glass.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isMe) Color.White.copy(alpha = 0.6f) else colors.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        msg.repliedToText,
                        fontSize = 12.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.85f) else textColor.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Main bubble
        Box {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bubbleColor, RoundedCornerShape(16.dp))
                    .clickable { showMenu = true }
                    .padding(10.dp),
                color = Color.Transparent
            ) {
                Column {
                    // Voice message
                    if (!msg.voiceUrl.isNullOrEmpty()) {
                        VoiceMessagePlayer(
                            voiceUrl = msg.voiceUrl,
                            textColor = textColor,
                            isMe = isMe,
                            colors = colors
                        )
                    }
                    // Image message
                    else if (!msg.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = msg.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Text message
                    else {
                        Text(
                            msg.text,
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }

                    // Timestamp + edited label
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (msg.edited) {
                            Text(
                                "edited",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.55f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text(
                            formatTime(msg.timestamp),
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Emoji reaction display
            if (!msg.emojiReaction.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(if (isMe) Alignment.BottomStart else Alignment.BottomEnd)
                        .offset(y = (-4).dp, x = if (isMe) (-8).dp else 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(msg.emojiReaction, fontSize = 16.sp)
                }
            }

            // Dropdown menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Reply") },
                    onClick = {
                        onReply(msg)
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Filled.Reply, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Forward") },
                    onClick = {
                        onForward(msg)
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Filled.Forward, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("React") },
                    onClick = {
                        showEmojiPicker = true
                        showMenu = false
                    },
                    leadingIcon = { Text("\u2764\uFE0F") }
                )
                if (isMe) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEdit(msg)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Reply, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }

            // Emoji picker sub-dialog
            if (showEmojiPicker) {
                EmojiPickerDialog(
                    onDismiss = { showEmojiPicker = false },
                    onEmojiSelected = { emoji ->
                        onReact(emoji)
                        showEmojiPicker = false
                    }
                )
            }
        }
    }
}

// ============================================================
// EMOJI PICKER DIALOG
// ============================================================

@Composable
fun EmojiPickerDialog(
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    val colors = LocalThemeColors.current
    val emojiOptions = listOf("\u2764\uFE0F", "\uD83D\uDD25", "\uD83D\uDE02", "\uD83D\uDE22", "\u2728")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("React", fontWeight = FontWeight.SemiBold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                emojiOptions.forEach { emoji ->
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onEmojiSelected(emoji) },
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 28.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp)
    )
}

// ============================================================
// FORWARD DIALOG
// ============================================================

@Composable
fun ForwardDialog(
    currentUserId: String,
    allUsers: List<UserProfile>,
    onDismiss: () -> Unit,
    onForward: (String) -> Unit
) {
    val colors = LocalThemeColors.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(allUsers, searchQuery) {
        allUsers.filter { user ->
            user.uid != currentUserId && (
                searchQuery.isBlank() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true)
                )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forward to...", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search users...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(filteredUsers) { user ->
                        Surface(
                            onClick = { onForward(user.uid) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (user.profilePicture.isNotEmpty()) {
                                    AsyncImage(
                                        model = user.profilePicture,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            user.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(user.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("@${user.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (filteredUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp)
    )
}

// ============================================================
// VOICE MESSAGE PLAYER
// ============================================================

@Composable
fun VoiceMessagePlayer(
    voiceUrl: String,
    textColor: Color,
    isMe: Boolean,
    colors: com.example.ui.theme.ThemeColors
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    val scope = rememberCoroutineScope()

    DisposableEffect(voiceUrl) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Play/Pause button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isMe) Color.White.copy(alpha = 0.25f) else colors.primary.copy(alpha = 0.15f))
                .clickable {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        try {
                            mediaPlayer.reset()
                            mediaPlayer.setDataSource(voiceUrl)
                            mediaPlayer.setOnErrorListener { _, _, _ ->
                                isPlaying = false
                                progress = 0f
                                Toast.makeText(context, "Playback error", Toast.LENGTH_SHORT).show()
                                true
                            }
                            mediaPlayer.prepareAsync()
                            mediaPlayer.setOnPreparedListener { mp ->
                                mp.start()
                                isPlaying = true
                                scope.launch {
                                    val totalDuration = mp.duration.toFloat()
                                    while (isPlaying && mp.isPlaying) {
                                        progress = mp.currentPosition.toFloat() / totalDuration
                                        delay(100)
                                    }
                                    if (!mp.isPlaying) {
                                        isPlaying = false
                                        progress = 0f
                                    }
                                }
                            }
                            mediaPlayer.setOnCompletionListener {
                                isPlaying = false
                                progress = 0f
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Waveform / progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isMe) Color.White.copy(alpha = 0.15f) else colors.glass.copy(alpha = 0.3f))
        ) {
            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isMe) Color.White.copy(alpha = 0.3f) else colors.primary.copy(alpha = 0.25f))
            )
            // Waveform bars
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val barCount = 24
                repeat(barCount) { index ->
                    val barHeight = when {
                        index % 5 == 0 -> 20.dp
                        index % 3 == 0 -> 14.dp
                        else -> 8.dp
                    }
                    val isActive = (index.toFloat() / barCount) <= progress
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                textColor.copy(
                                    alpha = if (isActive) 0.9f else 0.3f
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Duration placeholder
        Text(
            if (isPlaying) "${(progress * 100).toInt()}%" else "0:00",
            fontSize = 11.sp,
            color = textColor.copy(alpha = 0.7f)
        )
    }
}
