package com.example

import android.Manifest
import android.media.MediaPlayer
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mute
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ForestGlass
import com.example.ui.theme.LavenderGlass
import com.example.ui.theme.MidnightGlass
import com.example.ui.theme.OceanGlass
import com.example.ui.theme.PinkGlass
import com.example.ui.theme.RoseGoldGlass
import com.example.ui.theme.SunsetGlass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.tasks.await

// ═══════════════════════════════════════════════════════════════
// 1. COMMUNITY SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: androidx.navigation.NavHostController) {
    val allGroups by FirebaseManager.getGroups().collectAsState(initial = emptyList())
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    val myGroups by FirebaseManager.getUserGroups(currentUserId).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var groupPictureBytes by remember { mutableStateOf<ByteArray?>(null) }
    var groupPicturePreview by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    groupPictureBytes = bytes
                    groupPicturePreview = it.toString()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to pick image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredAllGroups = allGroups.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
    }

    val discoverGroups = filteredAllGroups.filter { group ->
        !group.members.contains(currentUserId)
    }

    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        var isUploading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Create New Group",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Group picture upload
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (groupPicturePreview != null) {
                            AsyncImage(
                                model = groupPicturePreview,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "Upload group picture",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        "Tap to add photo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank() && auth.currentUser != null) {
                            isUploading = true
                            scope.launch {
                                try {
                                    val pictureUrl = if (groupPictureBytes != null) {
                                        SupabaseManager.uploadFile(
                                            "group_images",
                                            "${UUID.randomUUID()}.jpg",
                                            groupPictureBytes!!
                                        )
                                    } else ""
                                    FirebaseManager.createGroup(
                                        groupName.trim(),
                                        groupDesc.trim(),
                                        auth.currentUser!!.uid,
                                        pictureUrl
                                    )
                                    isUploading = false
                                    showCreateDialog = false
                                    groupName = ""
                                    groupDesc = ""
                                    groupPictureBytes = null
                                    groupPicturePreview = null
                                } catch (e: Exception) {
                                    isUploading = false
                                    Toast.makeText(
                                        context,
                                        "Failed: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = groupName.isNotBlank() && !isUploading,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isUploading) {
                        Text("Creating...")
                    } else {
                        Text("Create")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Community",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = {
                        // Toggle search focus
                    }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search groups",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    groupPictureBytes = null
                    groupPicturePreview = null
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.GroupAdd, contentDescription = "Create Group")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search groups...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // My Groups Section
                if (myGroups.isNotEmpty() && searchQuery.isBlank()) {
                    item {
                        Text(
                            "My Groups",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(myGroups) { group ->
                        GroupListItem(
                            group = group,
                            isMember = true,
                            onClick = {
                                navController.navigate("group_detail/${group.groupId}/${group.name}")
                            },
                            onJoin = {}
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                // Discover Section
                item {
                    Text(
                        if (searchQuery.isNotBlank()) "Search Results" else "Discover",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }

                if (discoverGroups.isEmpty() && searchQuery.isBlank()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Group,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No groups to discover yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Text(
                                "Create one or wait for others!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else if (filteredAllGroups.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No groups found for \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    val displayGroups = if (searchQuery.isNotBlank()) {
                        filteredAllGroups.filter { !myGroups.any { mg -> mg.groupId == it.groupId } }
                    } else {
                        discoverGroups
                    }
                    items(displayGroups) { group ->
                        GroupListItem(
                            group = group,
                            isMember = group.members.contains(currentUserId),
                            onClick = {
                                navController.navigate("group_detail/${group.groupId}/${group.name}")
                            },
                            onJoin = {
                                if (currentUserId.isNotBlank()) {
                                    FirebaseManager.addMemberToGroup(group.groupId, currentUserId)
                                    Toast.makeText(
                                        context,
                                        "Joined ${group.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupListItem(
    group: FirebaseManager.Group,
    isMember: Boolean,
    onClick: () -> Unit,
    onJoin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Group avatar
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (group.groupPicture.isNotEmpty()) {
                AsyncImage(
                    model = group.groupPicture,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    group.name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                group.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                group.description.ifBlank { "No description" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${group.members.size} member${if (group.members.size != 1) "s" else ""}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (!isMember) {
            Button(
                onClick = onJoin,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Join", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. GROUP CHAT DETAIL SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GroupChatDetailScreen(
    navController: androidx.navigation.NavHostController,
    groupId: String,
    groupName: String
) {
    var message by remember { mutableStateOf("") }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: return
    val messages by FirebaseManager.getGroupMessages(groupId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    val allGroups by FirebaseManager.getGroups().collectAsState(initial = emptyList())
    val currentGroup = allGroups.find { it.groupId == groupId }
    val isAdmin = currentGroup?.adminId == currentUserId
    val memberCount = currentGroup?.members?.size ?: 0

    // Sender names cache
    val senderNames = remember { mutableMapOf<String, String>() }
    val senderPictures = remember { mutableMapOf<String, String>() }

    LaunchedEffect(messages) {
        val senderIds = messages.map { it.senderId }.distinct().filter { it != currentUserId }
        for (senderId in senderIds) {
            if (!senderNames.containsKey(senderId)) {
                try {
                    val doc = FirebaseManager.firestore.collection("users")
                        .document(senderId).get().await()
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        senderNames[senderId] = profile.name
                        senderPictures[senderId] = profile.profilePicture
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val url = SupabaseManager.uploadFile(
                            "group_images",
                            "${UUID.randomUUID()}.jpg",
                            bytes
                        )
                        FirebaseManager.sendGroupMessage(
                            groupId, currentUserId, "", imageUrl = url
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            groupId = groupId,
            currentMembers = currentGroup?.members ?: emptyList(),
            onDismiss = { showAddMemberDialog = false }
        )
    }

    // Leave group confirmation
    var showLeaveDialog by remember { mutableStateOf(false) }
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Leave Group", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave $groupName?") },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseManager.removeMemberFromGroup(groupId, currentUserId)
                        showLeaveDialog = false
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete group confirmation
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete Group", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete \"$groupName\". This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseManager.deleteGroup(groupId)
                        showDeleteDialog = false
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            groupName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$memberCount member${if (memberCount != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Member") },
                                onClick = {
                                    showMoreMenu = false
                                    showAddMemberDialog = true
                                },
                                leadingIcon = { Icon(Icons.Filled.PersonAdd, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Leave Group") },
                                onClick = {
                                    showMoreMenu = false
                                    showLeaveDialog = true
                                },
                                leadingIcon = { Icon(Icons.Filled.ExitToApp, null) }
                            )
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Delete Group") },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Delete,
                                            null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image button
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = "Send image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Text input
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Message group...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    // Voice / Send
                    if (message.isBlank() && !isRecording) {
                        IconButton(onClick = {
                            if (recordAudioPermission.status.isGranted) {
                                try {
                                    isRecording = true
                                    recorder.startRecording()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Recorder error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                recordAudioPermission.launchPermissionRequest()
                            }
                        }) {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = "Record voice",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (isRecording) {
                        // Recording indicator
                        val infiniteTransition = rememberInfiniteTransition(label = "recording")
                        val pulse by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        IconButton(onClick = {
                            try {
                                isRecording = false
                                val file = recorder.stopRecording()
                                if (file != null) {
                                    coroutineScope.launch {
                                        try {
                                            val url = SupabaseManager.uploadFile(
                                                "group_voices",
                                                "${UUID.randomUUID()}.mp3",
                                                file.readBytes()
                                            )
                                            FirebaseManager.sendGroupMessage(
                                                groupId,
                                                currentUserId,
                                                "",
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
                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Icon(
                                Icons.Filled.Stop,
                                contentDescription = "Stop recording",
                                tint = Color.Red
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            if (message.isNotBlank()) {
                                FirebaseManager.sendGroupMessage(
                                    groupId,
                                    currentUserId,
                                    message.trim()
                                )
                                message = ""
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Group,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No messages yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                    Text(
                        "Start the conversation!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(messages.size) { index ->
                        val msg = messages[index]
                        val isMe = msg.senderId == currentUserId
                        val senderName = if (isMe) "You" else senderNames[msg.senderId] ?: "Loading..."
                        val senderPic = senderPictures[msg.senderId] ?: ""

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            // Show sender name for group chat
                            if (!isMe) {
                                Row(
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (senderPic.isNotEmpty()) {
                                        AsyncImage(
                                            model = senderPic,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        senderName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            MessageBubble(
                                msg = msg,
                                isMe = isMe,
                                onReply = {},
                                onDelete = {
                                    if (isMe) {
                                        FirebaseManager.deleteGroupMessage(
                                            groupId,
                                            msg.messageId
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by FirebaseManager.getCurrentUserProfile().collectAsState(initial = null)
    val isDarkTheme by PreferenceManager.isDarkTheme(context).collectAsState(initial = true)
    val currentThemeKey by PreferenceManager.getTheme(context).collectAsState(initial = AppTheme.PINK_GLASS.key)
    val soundEnabled by PreferenceManager.isNotificationSoundEnabled(context).collectAsState(initial = true)
    val pushEnabled by PreferenceManager.isNotificationEnabled(context).collectAsState(initial = true)
    val blockedUserIds by FirebaseManager.getBlockedUsers(FirebaseManager.auth.currentUser?.uid ?: "")
        .collectAsState(initial = emptyList())

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showBlockedUsersSheet by remember { mutableStateOf(false) }

    val profilePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null && userProfile != null) {
                        val url = SupabaseManager.uploadFile(
                            "profile_pictures",
                            "${userProfile!!.uid}_${UUID.randomUUID()}.jpg",
                            bytes
                        )
                        FirebaseManager.updateUserProfile(
                            userProfile!!.uid,
                            null,
                            null,
                            url
                        )
                        Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    if (showEditProfileDialog && userProfile != null) {
        EditProfileDialog(
            currentName = userProfile!!.name,
            currentBio = userProfile!!.bio,
            uid = userProfile!!.uid,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showBlockedUsersSheet) {
        BlockedUsersSheet(
            blockedUserIds = blockedUserIds,
            onDismiss = { showBlockedUsersSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ─── Profile Section ───
            item {
                SectionHeader("Profile")
            }
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with tap to change
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                )
                                .clickable { profilePictureLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile?.profilePicture?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = userProfile!!.profilePicture,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    (userProfile?.name?.take(1) ?: "?").uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                )
                            }
                            // Edit overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit picture",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Name - tap to edit
                        Column(
                            modifier = Modifier.clickable { showEditProfileDialog = true },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                userProfile?.name ?: "Loading...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Edit profile",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // Bio
                        Text(
                            userProfile?.bio?.ifBlank { "Tap edit to add a bio" } ?: "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Info rows
                        SettingsInfoRow(
                            icon = { Icon(Icons.Filled.Person, null, Modifier.size(20.dp)) },
                            label = "Username",
                            value = "@${userProfile?.username ?: ""}"
                        )
                        SettingsInfoRow(
                            icon = { Icon(Icons.Filled.Notifications, null, Modifier.size(20.dp)) },
                            label = "Email",
                            value = FirebaseAuth.getInstance().currentUser?.email ?: ""
                        )
                        SettingsInfoRow(
                            icon = { Icon(Icons.Filled.Palette, null, Modifier.size(20.dp)) },
                            label = "Date of Birth",
                            value = userProfile?.dob?.ifBlank { "Not set" } ?: "Not set"
                        )
                        SettingsInfoRow(
                            icon = { Icon(Icons.Filled.Person, null, Modifier.size(20.dp)) },
                            label = "Gender",
                            value = userProfile?.gender?.ifBlank { "Not set" } ?: "Not set"
                        )
                    }
                }
            }

            // ─── Appearance Section ───
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Appearance")
            }
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Theme picker
                        Text(
                            "Theme",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val themeOptions = listOf(
                            AppTheme.PINK_GLASS to PinkGlass,
                            AppTheme.OCEAN to OceanGlass,
                            AppTheme.SUNSET to SunsetGlass,
                            AppTheme.FOREST to ForestGlass,
                            AppTheme.LAVENDER to LavenderGlass,
                            AppTheme.MIDNIGHT to MidnightGlass,
                            AppTheme.ROSE_GOLD to RoseGoldGlass
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(themeOptions) { (theme, glassColor) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            PreferenceManager.setTheme(context, theme.key)
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(glassColor, CircleShape)
                                            .then(
                                                if (theme.key == currentThemeKey) {
                                                    Modifier.border(
                                                        3.dp,
                                                        MaterialTheme.colorScheme.onSurface,
                                                        CircleShape
                                                    )
                                                } else {
                                                    Modifier.border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant,
                                                        CircleShape
                                                    )
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (theme.key == currentThemeKey) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = "Selected",
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        theme.label,
                                        fontSize = 10.sp,
                                        color = if (theme.key == currentThemeKey)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Dark mode toggle
                        SettingsToggleRow(
                            icon = {
                                Icon(
                                    if (isDarkTheme) Icons.Filled.Palette else Icons.Filled.Palette,
                                    null,
                                    Modifier.size(20.dp)
                                )
                            },
                            label = "Dark Mode",
                            checked = isDarkTheme,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    PreferenceManager.setDarkTheme(context, it)
                                }
                            }
                        )
                    }
                }
            }

            // ─── Notifications Section ───
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Notifications")
            }
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SettingsToggleRow(
                            icon = {
                                Icon(
                                    if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                    null,
                                    Modifier.size(20.dp)
                                )
                            },
                            label = "Notification Sound",
                            checked = soundEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    PreferenceManager.setNotificationSound(context, it)
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        SettingsToggleRow(
                            icon = {
                                Icon(
                                    Icons.Filled.Notifications,
                                    null,
                                    Modifier.size(20.dp)
                                )
                            },
                            label = "Push Notifications",
                            checked = pushEnabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    PreferenceManager.setNotificationEnabled(context, it)
                                }
                            }
                        )
                    }
                }
            }

            // ─── Privacy Section ───
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Privacy")
            }
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBlockedUsersSheet = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Block,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Blocked Users",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${blockedUserIds.size} user${if (blockedUserIds.size != 1) "s" else ""} blocked",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "View",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ─── Account Section ───
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Account")
            }
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // Logout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    FirebaseAuth.getInstance().signOut()
                                    navController.navigate("login") { popUpTo(0) }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                "Logout",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // App version
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "App Version",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "2.0.0",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: @Composable () -> Unit,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 3b. EDIT PROFILE DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun EditProfileDialog(
    currentName: String,
    currentBio: String,
    uid: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Edit Profile", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        isSaving = true
                        scope.launch {
                            try {
                                FirebaseManager.updateUserProfile(
                                    uid,
                                    name.trim(),
                                    bio.trim(),
                                    null
                                )
                                isSaving = false
                                onDismiss()
                                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT)
                                    .show()
                            } catch (e: Exception) {
                                isSaving = false
                                Toast.makeText(
                                    context,
                                    "Failed: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                enabled = name.isNotBlank() && !isSaving,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isSaving) onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// 3c. BLOCKED USERS SHEET
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersSheet(
    blockedUserIds: List<String>,
    onDismiss: () -> Unit
) {
    val currentUserId = FirebaseManager.auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val userProfiles = remember { mutableStateMapOf<String, UserProfile>() }

    LaunchedEffect(blockedUserIds) {
        for (uid in blockedUserIds) {
            if (!userProfiles.containsKey(uid)) {
                try {
                    val doc = FirebaseManager.firestore.collection("users").document(uid).get().await()
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) userProfiles[uid] = profile
                } catch (_: Exception) {}
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Blocked Users",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (blockedUserIds.isEmpty()) {
                Text(
                    "No blocked users",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 32.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                blockedUserIds.forEach { uid ->
                    val profile = userProfiles[uid]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile?.profilePicture?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = profile.profilePicture,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    (profile?.name?.take(1) ?: "?").uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile?.name ?: "Loading...",
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(
                                profile?.username ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                FirebaseManager.unblockUser(currentUserId, uid)
                            }
                        ) {
                            Text(
                                "Unblock",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════
// 5. STORY VIEWER SCREEN
// ═══════════════════════════════════════════════════════════════

@Composable
fun StoryViewerScreen(
    navController: androidx.navigation.NavHostController,
    userId: String,
    initialStoryIndex: Int = 0
) {
    val stories by FirebaseManager.getUserStories(userId).collectAsState(initial = emptyList())
    var currentIndex by remember { mutableStateOf(initialStoryIndex.coerceIn(0, maxOf(0, (stories.size - 1).coerceAtLeast(0)))) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val currentUserId = FirebaseManager.auth.currentUser?.uid ?: ""

    // User profile for header
    val userProfile by FirebaseManager.getUserProfile(userId).collectAsState(initial = null)

    // Video player state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Auto-advance timer (5 seconds)
    LaunchedEffect(currentIndex, isPaused, stories) {
        if (stories.isEmpty()) return@LaunchedEffect
        progress = 0f
        if (!isPaused) {
            val story = stories.getOrNull(currentIndex) ?: return@LaunchedEffect
            val duration = if (story.mediaType == "video") {
                // For video, we just use 5 seconds as a fallback if video doesn't load
                5000L
            } else {
                5000L
            }
            val steps = 60
            val stepDuration = duration / steps
            for (i in 1..steps) {
                if (!isPaused) {
                    delay(stepDuration)
                    progress = i.toFloat() / steps
                }
            }
            // Auto-advance to next
            if (currentIndex < stories.size - 1) {
                currentIndex++
            } else {
                // All stories viewed, go back
                navController.navigateUp()
            }
        }
    }

    // Handle video playback
    val currentStory = stories.getOrNull(currentIndex)
    DisposableEffect(currentStory?.storyId) {
        if (currentStory != null && currentStory.mediaType == "video") {
            try {
                val player = MediaPlayer()
                player.setDataSource(context, currentStory.mediaUrl.toUri())
                player.prepareAsync()
                player.setOnPreparedListener { mp ->
                    mp.start()
                    mediaPlayer = mp
                }
                player.setOnCompletionListener {
                    if (currentIndex < stories.size - 1) {
                        currentIndex++
                    } else {
                        navController.navigateUp()
                    }
                }
                onDispose {
                    try {
                        player.release()
                    } catch (_: Exception) {}
                    mediaPlayer = null
                }
            } catch (_: Exception) {}
        } else {
            onDispose {
                try { mediaPlayer?.release() } catch (_: Exception) {}
                mediaPlayer = null
            }
        }
    }

    if (stories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No stories available", color = Color.White)
        }
        return
    }

    val story = stories[currentIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Story content (image or video)
        if (story.mediaType == "video") {
            AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        setVideoURI(story.mediaUrl.toUri())
                        setOnPreparedListener { mp -> mp.start() }
                        setOnCompletionListener {
                            if (currentIndex < stories.size - 1) {
                                currentIndex++
                            } else {
                                navController.navigateUp()
                            }
                        }
                        start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
                .align(Alignment.TopCenter)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .align(Alignment.BottomCenter)
        )

        // Close button
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 8.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Progress bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 8.dp, end = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            stories.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                            .graphicsLayer {
                                scaleX = when {
                                    index < currentIndex -> 1f
                                    index == currentIndex -> progress
                                    else -> 0f
                                }
                            }
                    )
                }
            }
        }

        // User info at top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 52.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile?.profilePicture?.isNotEmpty() == true) {
                    AsyncImage(
                        model = userProfile!!.profilePicture,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        (userProfile?.name?.take(1) ?: "?").uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    userProfile?.name ?: "Loading...",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    formatTime(story.timestamp),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        // Tap zones: left = previous, right = next; long press = pause
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onLongPress = { isPaused = true },
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 3) {
                                // Left tap - previous
                                if (currentIndex > 0) {
                                    currentIndex--
                                }
                            } else {
                                // Right/center tap - next
                                if (currentIndex < stories.size - 1) {
                                    currentIndex++
                                } else {
                                    navController.navigateUp()
                                }
                            }
                        }
                    )
                }
        )

        // Pause indicator
        if (isPaused) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⏸",
                    fontSize = 28.sp
                )
            }
        }

        // Reaction emoji bar at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val emojis = listOf("❤️", "🔥", "😂", "😮", "😢")
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            if (currentUserId.isNotBlank()) {
                                FirebaseManager.addStoryReaction(
                                    story.storyId,
                                    currentUserId,
                                    emoji
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 6. USER PROFILE SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: androidx.navigation.NavHostController,
    userId: String
) {
    val userProfile by FirebaseManager.getUserProfile(userId).collectAsState(initial = null)
    val posts by FirebaseManager.getUserPosts(userId).collectAsState(initial = emptyList())
    val stories by FirebaseManager.getUserStories(userId).collectAsState(initial = emptyList())
    val onlineStatus by FirebaseManager.getUserOnlineStatus(userId).collectAsState(initial = false to 0L)
    val currentUserId = FirebaseManager.auth.currentUser?.uid ?: ""
    val isBlocked by FirebaseManager.isBlocked(currentUserId, userId).collectAsState(initial = false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mutedUsers by PreferenceManager.getMutedUsers(context).collectAsState(initial = emptySet())
    val isMuted = mutedUsers.contains(userId)

    // Reactions received calculation
    val totalReactions = remember(posts) {
        posts.sumOf { it.reactions.size }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        userProfile?.name ?: "Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        if (userProfile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
            return@Scaffold
        }

        val profile = userProfile!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header with avatar, name, bio
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with online status
                    Box {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.profilePicture.isNotEmpty()) {
                                AsyncImage(
                                    model = profile.profilePicture,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    profile.name.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                )
                            }
                        }
                        // Online indicator
                        if (onlineStatus.first) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        profile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "@${profile.username}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        profile.bio.ifBlank { "No bio yet" },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // Stats
            item {
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Posts",
                            value = posts.size.toString()
                        )
                        StatItem(
                            label = "Reactions",
                            value = totalReactions.toString()
                        )
                        StatItem(
                            label = "Stories",
                            value = stories.size.toString()
                        )
                    }
                }
            }

            // Stories ring row
            if (stories.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader("Stories")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(stories) { story ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        val storyIndex = stories.indexOf(story)
                                        navController.navigate("story_viewer/$userId/$storyIndex")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = story.mediaUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                            )
                                        )
                                )
                                Text(
                                    formatTime(story.timestamp),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Send message
                    Button(
                        onClick = {
                            navController.navigate("chat_detail/$userId/${profile.name}")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Message", fontWeight = FontWeight.SemiBold)
                    }

                    // Block/Unblock
                    Button(
                        onClick = {
                            if (isBlocked) {
                                FirebaseManager.unblockUser(currentUserId, userId)
                                Toast.makeText(context, "Unblocked ${profile.name}", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                FirebaseManager.blockUser(currentUserId, userId)
                                Toast.makeText(context, "Blocked ${profile.name}", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBlocked)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(
                            if (isBlocked) Icons.Filled.CheckCircle else Icons.Filled.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isBlocked) "Unblock" else "Block",
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    // Mute
                    IconButton(
                        onClick = {
                            scope.launch {
                                PreferenceManager.toggleMuteUser(context, userId)
                                Toast.makeText(
                                    context,
                                    if (!isMuted) "Muted ${profile.name}" else "Unmuted ${profile.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isMuted)
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            Icons.Filled.Mute,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Posts
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Posts")
            }
            if (posts.isEmpty()) {
                item {
                    Text(
                        "No posts yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(posts) { post ->
                    PostCard(
                        post = post,
                        userName = profile.name,
                        userPicture = profile.profilePicture,
                        onClick = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

    post: FirebaseManager.Post,
    userName: String,
    userPicture: String,
    onClick: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (userPicture.isNotEmpty()) {
                        AsyncImage(
                            model = userPicture,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            userName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        userName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        formatTime(post.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Post image
            if (post.imageUrl != null) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
            }

            // Caption
            if (post.caption.isNotBlank()) {
                Text(
                    post.caption,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Tags
            if (post.tags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(post.tags) { tag ->
                        Text(
                            "#$tag",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Reactions + views
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reaction emoji summary
                val reactionEmojis = post.reactions.values.distinct().take(3)
                if (reactionEmojis.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        reactionEmojis.forEach { emoji ->
                            Text(emoji, fontSize = 14.sp)
                        }
                        Text(
                            " ${post.reactions.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (post.viewCount > 0) {
                    Text(
                        "${post.viewCount} views",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 7. ADD MEMBER DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun AddMemberDialog(
    groupId: String,
    currentMembers: List<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val allUsers by FirebaseManager.searchUsers(searchQuery).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Filter out current members
    val availableUsers = allUsers.filter { !currentMembers.contains(it.uid) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Add Members", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search users...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (availableUsers.isEmpty()) {
                    Text(
                        if (searchQuery.isBlank()) "No users available" else "No users found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(availableUsers) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        FirebaseManager.addMemberToGroup(groupId, user.uid)
                                        Toast.makeText(
                                            context,
                                            "Added ${user.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (user.profilePicture.isNotEmpty()) {
                                        AsyncImage(
                                            model = user.profilePicture,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            user.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        user.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "@${user.username}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Filled.PersonAdd,
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// 8. FORWARD DIALOG
// ═══════════════════════════════════════════════════════════════

    message: FirebaseManager.Message,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val allUsers by FirebaseManager.searchUsers(searchQuery).collectAsState(initial = emptyList())
    val userChats by FirebaseManager.getUserChats(currentUserId).collectAsState(initial = emptyList())
    var isForwarding by remember { mutableStateOf(false) }

    // Build a unique list of chat targets
    val chatTargets = remember(allUsers, userChats) {
        val targetIds = mutableSetOf<String>()
        userChats.forEach { targetIds.add(it.otherUserId) }
        allUsers.forEach { if (it.uid != currentUserId) targetIds.add(it.uid) }
        targetIds
    }

    // Get profiles for chat targets
    val targetProfiles = remember { mutableStateMapOf<String, UserProfile>() }
    LaunchedEffect(chatTargets) {
        for (uid in chatTargets) {
            if (!targetProfiles.containsKey(uid)) {
                try {
                    val doc = FirebaseManager.firestore.collection("users").document(uid).get().await()
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) targetProfiles[uid] = profile
                } catch (_: Exception) {}
            }
        }
    }

    // Filter by search
    val filteredTargets = if (searchQuery.isBlank()) {
        chatTargets.toList()
    } else {
        chatTargets.filter { uid ->
            val p = targetProfiles[uid]
            p?.name?.contains(searchQuery, ignoreCase = true) == true ||
                p?.username?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isForwarding) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Forward Message", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Message preview
                if (message.text.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            message.text,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (message.imageUrl != null) {
                    Text(
                        "📷 Photo message",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else if (message.voiceUrl != null) {
                    Text(
                        "🎙️ Voice message",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (filteredTargets.isEmpty()) {
                    Text(
                        "No contacts found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredTargets) { uid ->
                            val profile = targetProfiles[uid]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isForwarding) {
                                        if (profile != null) {
                                            isForwarding = true
                                            val chatId = FirebaseManager.getChatId(
                                                currentUserId,
                                                uid
                                            )
                                            FirebaseManager.forwardMessage(
                                                originalMessage = message,
                                                toChatId = chatId,
                                                senderId = currentUserId,
                                                receiverId = uid
                                            )
                                            Toast.makeText(
                                                context,
                                                "Forwarded to ${profile.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isForwarding = false
                                            onDismiss()
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profile?.profilePicture?.isNotEmpty() == true) {
                                        AsyncImage(
                                            model = profile.profilePicture,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            (profile?.name?.take(1) ?: "?").uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        profile?.name ?: "Loading...",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "@${profile?.username ?: ""}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Forward",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!isForwarding) onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

// formatTime is defined in MainActivity and accessible globally