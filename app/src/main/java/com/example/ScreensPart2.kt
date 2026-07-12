package com.example

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.LocalThemeColors
import com.example.ui.theme.StoryRingGradient
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

// ===== EMOJI REACTION CONSTANTS =====
private val REACTION_EMOJIS = listOf("❤️", "🔥", "😂", "😢", "😮")
private val REACTION_LABELS = mapOf(
    "❤️" to "Heart",
    "🔥" to "Fire",
    "😂" to "Laugh",
    "😢" to "Sad",
    "😮" to "Wow"
)

// ============================================================================
// HOME / FEED SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    val currentUserProfile by FirebaseManager.getCurrentUserProfile().collectAsState(initial = null)

    val posts by FirebaseManager.getPosts().collectAsState(initial = emptyList())
    val stories by FirebaseManager.getStories().collectAsState(initial = emptyList())
    val allUsers by FirebaseManager.getAllUsersFlow().collectAsState(initial = emptyList())

    var showCommentsForPost by remember { mutableStateOf<String?>(null) }

    val themeColors = LocalThemeColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "EB Chat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("notifications")
                    }) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Notifications",
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
                onClick = { navController.navigate("create_post") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.AddCircle, contentDescription = "Create Post")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stories bar
            item {
                HomeStoriesBar(
                    stories = stories,
                    allUsers = allUsers,
                    onStoryClick = { /* Could open story viewer */ }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }

            // Feed posts
            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.ChatBubble,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No posts yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap + to create the first post!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(posts, key = { _, post -> post.postId }) { index, post ->
                    val user = allUsers.find { it.uid == post.userId }
                    LaunchedEffect(post.postId) {
                        FirebaseManager.incrementViewCount(post.postId)
                    }
                    PostCard(
                        post = post,
                        user = user,
                        currentUserId = currentUserId,
                        onPostClick = {
                            showCommentsForPost = post.postId
                        },
                        onReact = { emoji, isRemove ->
                            if (isRemove) {
                                FirebaseManager.removePostReaction(post.postId, currentUserId)
                            } else {
                                FirebaseManager.reactToPost(post.postId, currentUserId, emoji)
                            }
                        }
                    )
                    if (index < posts.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Comments bottom sheet
    if (showCommentsForPost != null) {
        CommentsSheet(
            postId = showCommentsForPost!!,
            currentUserId = currentUserId,
            currentUserName = currentUserProfile?.name ?: "",
            allUsers = allUsers,
            onDismiss = { showCommentsForPost = null }
        )
    }
}

// ============================================================================
// STORIES BAR (Enhanced with gradient ring)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeStoriesBar(
    stories: List<FirebaseManager.Story>,
    allUsers: List<UserProfile>,
    onStoryClick: (FirebaseManager.Story, UserProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val themeColors = LocalThemeColors.current

    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
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
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "My Story" add button
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clickable { imageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    // Gradient ring for "My Story"
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        themeColors.glass,
                                        themeColors.glassLight,
                                        themeColors.accent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(68f, 68f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AddCircle,
                                contentDescription = "Add Story",
                                modifier = Modifier.size(28.dp),
                                tint = themeColors.accent
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "My Story",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // User stories
        val userStories = stories.groupBy { it.userId }
        userStories.forEach { (userId, userStoriesList) ->
            val user = allUsers.find { it.uid == userId }
            if (user != null) {
                item(key = "story_$userId") {
                    HomeStoryItem(
                        user = user,
                        story = userStoriesList.first(),
                        themeColors = themeColors,
                        onClick = { onStoryClick(userStoriesList.first(), user) }
                    )
                }
            }
        }
    }
}

// ============================================================================
// STORY ITEM (Enhanced with gradient ring)
// ============================================================================

@Composable
fun HomeStoryItem(
    user: UserProfile,
    story: FirebaseManager.Story,
    themeColors: com.example.ui.theme.ThemeColors,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Outer gradient ring
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                StoryRingGradient,
                                themeColors.accent,
                                StoryRingGradient
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(68f, 68f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner border gap
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profilePicture.isNotEmpty()) {
                        AsyncImage(
                            model = user.profilePicture,
                            contentDescription = user.name,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
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
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            user.name.split(" ").firstOrNull() ?: "",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 72.dp),
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// POST CARD
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCard(
    post: FirebaseManager.Post,
    user: UserProfile?,
    currentUserId: String,
    onPostClick: () -> Unit,
    onReact: (String, Boolean) -> Unit
) {
    val themeColors = LocalThemeColors.current
    val myReaction = post.reactions[currentUserId] ?: ""

    // Group reactions by emoji and count them
    val reactionCounts = post.reactions.values
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPostClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 4.dp,
        border = BorderStroke(
            width = 1.dp,
            color = themeColors.glassBorder
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Avatar + Name + Time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar
                if (user != null && user.profilePicture.isNotEmpty()) {
                    AsyncImage(
                        model = user.profilePicture,
                        contentDescription = user.name,
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
                            (user?.name ?: "?").take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user?.name ?: "Unknown User",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatTimeAgo(post.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Title
            if (post.title.isNotBlank()) {
                Text(
                    text = post.title,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tags as chips
            if (post.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    post.tags.forEach { tag ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = {
                                Text(
                                    "#$tag",
                                    fontSize = 11.sp,
                                    color = themeColors.accent
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = themeColors.glass.copy(alpha = 0.15f),
                                selectedContainerColor = themeColors.glass
                            ),
                            border = BorderStroke(1.dp, themeColors.glassBorder)
                        )
                    }
                }
            }

            // Image
            if (!post.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Video thumbnail with play icon
            if (!post.videoUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    // Video thumbnail placeholder
                    AsyncImage(
                        model = post.videoUrl,
                        contentDescription = "Post video",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Play icon overlay
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play video",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Caption
            if (post.caption.isNotBlank()) {
                Text(
                    text = post.caption,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Reaction bar
            ReactionBar(
                reactionCounts = reactionCounts,
                myReaction = myReaction,
                onReact = onReact
            )

            // Footer: Comment count + View count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ChatBubble,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${post.commentCount} comments",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${post.viewCount} views",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ============================================================================
// REACTION BAR
// ============================================================================

@Composable
fun ReactionBar(
    reactionCounts: List<Map.Entry<String, Int>>,
    myReaction: String,
    onReact: (String, Boolean) -> Unit
) {
    val themeColors = LocalThemeColors.current
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        // Display existing reactions
        if (reactionCounts.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                reactionCounts.forEach { (emoji, count) ->
                    val isMyReaction = emoji == myReaction
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isMyReaction)
                            themeColors.glass.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isMyReaction)
                            BorderStroke(1.dp, themeColors.accent)
                        else
                            BorderStroke(0.5.dp, themeColors.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "$count",
                                fontSize = 12.sp,
                                fontWeight = if (isMyReaction) FontWeight.Bold else FontWeight.Normal,
                                color = if (isMyReaction)
                                    themeColors.accent
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Emoji picker row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                REACTION_EMOJIS.forEach { emoji ->
                    val isSelected = emoji == myReaction
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) themeColors.glass.copy(alpha = 0.5f) else Color.Transparent,
                        label = "reactionBg"
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .clickable { onReact(emoji, isSelected) }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            emoji,
                            fontSize = if (isSelected) 22.sp else 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMMENTS BOTTOM SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    postId: String,
    currentUserId: String,
    currentUserName: String,
    allUsers: List<UserProfile>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var commentText by remember { mutableStateOf("") }
    val comments by FirebaseManager.getComments(postId).collectAsState(initial = emptyList())
    val themeColors = LocalThemeColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            // Title
            Text(
                "Comments",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(
                color = themeColors.glassBorder,
                thickness = 1.dp
            )

            // Comments list
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.ChatBubble,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No comments yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.commentId }) { comment ->
                        val commenter = allUsers.find { it.uid == comment.userId }
                        CommentItem(
                            comment = comment,
                            commenter = commenter
                        )
                    }
                }
            }

            HorizontalDivider(
                color = themeColors.glassBorder,
                thickness = 1.dp
            )

            // Comment input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            "Write a comment...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.glassBorder,
                        cursorColor = themeColors.accent
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            FirebaseManager.addComment(
                                postId = postId,
                                userId = currentUserId,
                                text = commentText.trim(),
                                userName = currentUserName
                            )
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (commentText.isNotBlank())
                            themeColors.accent
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: FirebaseManager.Comment,
    commenter: UserProfile?
) {
    val themeColors = LocalThemeColors.current

    Row(modifier = Modifier.fillMaxWidth()) {
        // Avatar
        if (commenter != null && commenter.profilePicture.isNotEmpty()) {
            AsyncImage(
                model = commenter.profilePicture,
                contentDescription = commenter.name,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (commenter?.name ?: comment.userName).take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Name and time row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    commenter?.name ?: comment.userName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    formatTimeAgo(comment.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            // Comment text
            Text(
                comment.text,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// ============================================================================
// CREATE POST SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    val currentUserProfile by FirebaseManager.getCurrentUserProfile().collectAsState(initial = null)
    val themeColors = LocalThemeColors.current

    var title by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }

    val tagList = remember(tagsInput) {
        tagsInput
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    // Tagged users
    val taggedUserIds = remember { mutableStateListOf<String>() }
    val taggedUsers = remember(taggedUserIds) {
        // Will be populated from allUsers
        taggedUserIds.toList()
    }

    val allUsers by FirebaseManager.getAllUsersFlow().collectAsState(initial = emptyList())
    var userSearchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(userSearchQuery, allUsers, taggedUserIds) {
        allUsers.filter { user ->
            user.uid != currentUserId &&
                    !taggedUserIds.contains(user.uid) &&
                    (user.name.contains(userSearchQuery, ignoreCase = true) ||
                            user.username.contains(userSearchQuery, ignoreCase = true))
        }
    }

    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var uploadedVideoUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val url = SupabaseManager.uploadFile("posts", "${UUID.randomUUID()}.jpg", bytes)
                        uploadedImageUrl = url
                        Toast.makeText(context, "Image uploaded!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                isUploading = false
            }
        }
    }

    val videoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val url = SupabaseManager.uploadFile("posts", "${UUID.randomUUID()}.mp4", bytes)
                        uploadedVideoUrl = url
                        Toast.makeText(context, "Video uploaded!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Video upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Post",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Title field
            item {
                Text(
                    "Title",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.glassBorder,
                        cursorColor = themeColors.accent
                    ),
                    maxLines = 2
                )
            }

            // Tags field
            item {
                Text(
                    "Tags",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    placeholder = { Text("Separate with commas: fun, travel, food") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.glassBorder,
                        cursorColor = themeColors.accent
                    ),
                    maxLines = 2
                )
                // Show tags as chips
                if (tagList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tagList.forEach { tag ->
                            FilterChip(
                                selected = false,
                                onClick = { },
                                label = {
                                    Text(
                                        "#$tag",
                                        fontSize = 12.sp,
                                        color = themeColors.accent
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = themeColors.glass.copy(alpha = 0.15f),
                                    selectedContainerColor = themeColors.glass
                                ),
                                border = BorderStroke(1.dp, themeColors.glassBorder)
                            )
                        }
                    }
                }
            }

            // Tag users
            item {
                Text(
                    "Tag Users",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = userSearchQuery,
                    onValueChange = { userSearchQuery = it },
                    placeholder = { Text("Search users to tag...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(Icons.Filled.Tag, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.glassBorder,
                        cursorColor = themeColors.accent
                    ),
                    singleLine = true
                )

                // Search results
                if (userSearchQuery.isNotBlank() && filteredUsers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, themeColors.glassBorder),
                        tonalElevation = 4.dp
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(filteredUsers) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            taggedUserIds.add(user.uid)
                                            userSearchQuery = ""
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (user.profilePicture.isNotEmpty()) {
                                        AsyncImage(
                                            model = user.profilePicture,
                                            contentDescription = user.name,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                user.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
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
                                }
                            }
                        }
                    }
                }

                // Tagged users chips
                if (taggedUserIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        taggedUserIds.forEach { uid ->
                            val taggedUser = allUsers.find { it.uid == uid }
                            AssistChip(
                                onClick = { taggedUserIds.remove(uid) },
                                label = {
                                    Text(
                                        taggedUser?.name ?: "User",
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.RemoveCircleOutline,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = themeColors.glass.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, themeColors.glassBorder)
                            )
                        }
                    }
                }
            }

            // Image upload
            item {
                Text(
                    "Media",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Image button
                    Button(
                        onClick = { imageLauncher.launch("image/*") },
                        enabled = !isUploading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uploadedImageUrl != null)
                                themeColors.glass.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = themeColors.accent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (uploadedImageUrl != null) "Image Added" else "Add Image",
                            fontSize = 13.sp
                        )
                    }
                    // Video button
                    Button(
                        onClick = { videoLauncher.launch("video/*") },
                        enabled = !isUploading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uploadedVideoUrl != null)
                                themeColors.glass.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = themeColors.accent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.VideoFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (uploadedVideoUrl != null) "Video Added" else "Add Video",
                            fontSize = 13.sp
                        )
                    }
                }

                // Show uploaded image preview
                if (uploadedImageUrl != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = uploadedImageUrl,
                            contentDescription = "Uploaded image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Remove button
                        IconButton(
                            onClick = { uploadedImageUrl = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Show uploaded video indicator
                if (uploadedVideoUrl != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, themeColors.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.VideoFile,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = themeColors.accent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Video uploaded successfully",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { uploadedVideoUrl = null }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove video",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Caption field
            item {
                Text(
                    "Caption",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Write a description...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.glassBorder,
                        cursorColor = themeColors.accent
                    )
                )
            }

            // Upload progress
            if (isUploading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = themeColors.accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Uploading...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Post button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank() || caption.isNotBlank()) {
                            isPosting = true
                            FirebaseManager.createPost(
                                userId = currentUserId,
                                title = title.trim(),
                                tags = tagList,
                                taggedUsers = taggedUserIds.toList(),
                                imageUrl = uploadedImageUrl,
                                videoUrl = uploadedVideoUrl,
                                caption = caption.trim()
                            )
                            Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Please add a title or caption", Toast.LENGTH_SHORT).show()
                        }
                        isPosting = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPosting && !isUploading && (title.isNotBlank() || caption.isNotBlank()),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                ) {
                    if (isPosting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Post", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ============================================================================
// NOTIFICATIONS SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavHostController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: ""
    val themeColors = LocalThemeColors.current

    val notifications by FirebaseManager.getUserNotifications(currentUserId)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "When someone interacts with you,\nyou'll see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(notifications, key = { it.notificationId }) { notification ->
                    NotificationItem(
                        notification = notification,
                        navController = navController,
                        themeColors = themeColors
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: FirebaseManager.AppNotification,
    navController: NavHostController,
    themeColors: com.example.ui.theme.ThemeColors
) {
    val iconInfo = getNotificationIcon(notification.type)
    val bgColor by animateColorAsState(
        targetValue = if (notification.isRead)
            Color.Transparent
        else
            themeColors.glass.copy(alpha = 0.15f),
        label = "notifBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Mark as read
                FirebaseManager.markNotificationRead(notification.notificationId)

                // Navigate based on type
                when (notification.type) {
                    "message" -> {
                        // Navigate to chat with sender
                        if (notification.senderId.isNotEmpty()) {
                            navController.navigate("chat_detail/${notification.senderId}/")
                        }
                    }
                    "comment", "reaction", "tag", "like" -> {
                        // Navigate to home feed
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                    else -> {
                        // Default: go back or stay
                    }
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = if (!notification.isRead)
            BorderStroke(1.dp, themeColors.glassBorder)
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconInfo.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconInfo.icon,
                    contentDescription = notification.type,
                    tint = iconInfo.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notification.title,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    notification.body,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatTimeAgo(notification.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Unread indicator dot
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(themeColors.accent)
                )
            }
        }
    }
}

private data class NotificationIconInfo(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
private fun getNotificationIcon(type: String): NotificationIconInfo {
    val themeColors = LocalThemeColors.current
    return when (type) {
        "message" -> NotificationIconInfo(
            icon = Icons.Filled.ChatBubble,
            color = MaterialTheme.colorScheme.primary
        )
        "comment" -> NotificationIconInfo(
            icon = Icons.Filled.ChatBubble,
            color = Color(0xFF2196F3)
        )
        "reaction", "like" -> NotificationIconInfo(
            icon = Icons.Filled.Favorite,
            color = Color(0xFFE91E63)
        )
        "tag" -> NotificationIconInfo(
            icon = Icons.Filled.Tag,
            color = Color(0xFFFF9800)
        )
        else -> NotificationIconInfo(
            icon = Icons.Filled.ThumbUp,
            color = themeColors.accent
        )
    }
}

// ============================================================================
// TIME FORMATTING HELPER
// ============================================================================

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        diff < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(diff) / 7}w ago"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff) / 30}mo ago"
    }
}