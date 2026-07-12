package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.ui.theme.AppTheme
import com.example.ui.theme.EBChatTheme
import com.example.ui.theme.LocalThemeColors
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"

data class OnboardingPage(
    val title: String,
    val description: String,
    val gradientColors: List<Color>
)

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseManager.context = this
        setContent {
            MyApp(intent, this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(intent: Intent, activity: ComponentActivity) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── Theme from PreferenceManager ──
    val isDarkTheme by PreferenceManager.isDarkTheme(context).collectAsState(initial = true)
    val themeKey by PreferenceManager.getTheme(context).collectAsState(
        initial = AppTheme.PINK_GLASS.key
    )
    val selectedTheme = remember(themeKey) {
        AppTheme.values().find { it.key == themeKey } ?: AppTheme.PINK_GLASS
    }

    // ── Notification permission ──
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Observe onboarding completed (wait for datastore) ──
    val onboardingCompleted by PreferenceManager.isOnboardingCompleted(context)
        .collectAsState(initial = null)

    EBChatTheme(
        theme = selectedTheme,
        darkTheme = isDarkTheme
    ) {
        val themeColors = LocalThemeColors.current
        val navController = rememberNavController()

        // ── Notification navigation intent extras ──
        val shouldOpenChat = intent.getBooleanExtra("openChat", false)
        val targetUserId = intent.getStringExtra("targetUserId") ?: ""
        val targetUserName = intent.getStringExtra("targetUserName") ?: ""
        val shouldOpenGroup = intent.getBooleanExtra("openGroup", false)
        val groupId = intent.getStringExtra("groupId") ?: ""
        val groupName = intent.getStringExtra("groupName") ?: ""
        val shouldOpenNotification = intent.getBooleanExtra("openNotification", false)

        // ── Determine start destination ──
        val currentUser = FirebaseManager.auth.currentUser
        val startDestination = when {
            onboardingCompleted == null -> "onboarding"
            onboardingCompleted == false -> "onboarding"
            currentUser != null -> "home"
            else -> "login"
        }

        // ── Bottom bar visibility ──
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val routesWithoutBottomBar = listOf(
            "onboarding",
            "login",
            "forgot_password",
            "chat_detail",
            "group_detail",
            "create_post",
            "story_viewer",
            "user_profile"
        )

        val showBottomBar = currentRoute != null &&
            routesWithoutBottomBar.none { route -> currentRoute.startsWith(route) }

        // ── Navigate on notification intent ──
        LaunchedEffect(startDestination) {
            if (startDestination == "home") {
                if (shouldOpenChat && targetUserId.isNotEmpty()) {
                    navController.navigate("chat_detail/$targetUserId/$targetUserName") {
                        popUpTo("home") { saveState = true }
                    }
                } else if (shouldOpenGroup && groupId.isNotEmpty()) {
                    navController.navigate("group_detail/$groupId/$groupName") {
                        popUpTo("home") { saveState = true }
                    }
                } else if (shouldOpenNotification) {
                    navController.navigate("notifications") {
                        popUpTo("home") { saveState = true }
                    }
                }
            }
        }

        // ── Update FCM token when user is logged in ──
        LaunchedEffect(currentUser) {
            if (currentUser != null) {
                try {
                    FirebaseManager.setPresence(currentUser.uid)
                    val token = com.google.firebase.messaging.FirebaseMessaging
                        .getInstance().token.await()
                    FirebaseManager.updateFcmToken(currentUser.uid, token)
                    com.google.firebase.messaging.FirebaseMessaging
                        .getInstance().subscribeToTopic("all_users")
                } catch (e: Exception) {
                    Log.w("MyApp", "FCM token update failed", e)
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    EBChatBottomNavigationBar(
                        navController = navController,
                        currentRoute = currentRoute,
                        themeColors = themeColors
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("onboarding") {
                    OnboardingScreen(navController = navController)
                }
                composable("login") {
                    LoginScreen(navController = navController)
                }
                composable("forgot_password") {
                    ForgotPasswordScreen(navController = navController)
                }
                composable("home") {
                    HomeScreen(navController = navController)
                }
                composable("chats") {
                    ChatsScreen(navController = navController)
                }
                composable("community") {
                    CommunityScreen(navController = navController)
                }
                composable("notifications") {
                    NotificationsScreen(navController = navController)
                }
                composable("settings") {
                    SettingsScreen(navController = navController)
                }
                composable(
                    route = "chat_detail/{targetUserId}/{targetUserName}",
                    arguments = listOf(
                        navArgument("targetUserId") { type = NavType.StringType },
                        navArgument("targetUserName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val tUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
                    val tUserName = backStackEntry.arguments?.getString("targetUserName") ?: ""
                    ChatDetailScreen(navController = navController, targetUserId = tUserId, targetUserName = tUserName)
                }
                composable(
                    route = "group_detail/{groupId}/{groupName}",
                    arguments = listOf(
                        navArgument("groupId") { type = NavType.StringType },
                        navArgument("groupName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val gId = backStackEntry.arguments?.getString("groupId") ?: ""
                    val gName = backStackEntry.arguments?.getString("groupName") ?: ""
                    GroupChatDetailScreen(navController = navController, groupId = gId, groupName = gName)
                }
                composable("create_post") {
                    CreatePostScreen(navController = navController)
                }
                composable(
                    route = "user_profile/{userId}",
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val profileUserId = backStackEntry.arguments?.getString("userId") ?: ""
                    UserProfileScreen(navController = navController, userId = profileUserId)
                }
                composable(
                    route = "story_viewer/{userId}/{storyIndex}",
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType },
                        navArgument("storyIndex") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val storyUserId = backStackEntry.arguments?.getString("userId") ?: ""
                    val storyIdx = backStackEntry.arguments?.getInt("storyIndex") ?: 0
                    StoryViewerScreen(navController = navController, userId = storyUserId, initialStoryIndex = storyIdx)
                }
            }
        }
    }
}

@Composable
fun EBChatBottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
    themeColors: com.example.ui.theme.ThemeColors
) {
    val items = listOf(
        BottomNavItem("home", "Home", Icons.Default.Home),
        BottomNavItem("chats", "Chats", Icons.Default.ChatBubble),
        BottomNavItem("community", "Community", Icons.Default.People),
        BottomNavItem("notifications", "Notifications", Icons.Default.Notifications),
        BottomNavItem("settings", "Settings", Icons.Default.Settings)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            }
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            },
                            fontSize = 11.sp
                        )
                    },
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  ONBOARDING SCREEN
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themeColors = LocalThemeColors.current

    val pages = listOf(
        OnboardingPage(
            title = "Welcome to EB Chat",
            description = "Connect with friends and family in a beautiful, secure messaging experience built for everyone.",
            gradientColors = listOf(
                Color(0xFFFF6B9D),
                Color(0xFFFFC3A0)
            )
        ),
        OnboardingPage(
            title = "Share Moments",
            description = "Share stories, photos, and voice messages with your loved ones in real-time.",
            gradientColors = listOf(
                Color(0xFFFF8ED4),
                Color(0xFFFFB6D9),
                Color(0xFFE040FB)
            )
        ),
        OnboardingPage(
            title = "Join Communities",
            description = "Discover vibrant communities, express yourself, and build meaningful connections.",
            gradientColors = listOf(
                Color(0xFFE040FB),
                Color(0xFFFF6B9D),
                Color(0xFFFF4081)
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ── Gradient background + pages ──
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex], pageIndex = pageIndex)
        }

        // ── Bottom overlay with indicators and buttons ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 32.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Next / Get Started button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        coroutineScope.launch {
                            PreferenceManager.setOnboardingCompleted(context)
                            navController.navigate("login") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = themeColors.primary
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Skip button
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            PreferenceManager.setOnboardingCompleted(context)
                            navController.navigate("login") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pageIndex: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_float")
    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = { it }),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = { it }),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )
    val float3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = { it }),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float3"
    )

    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(page.gradientColors)
            )
    ) {
        // ── Decorative animated gradient circles ──
        // Large circle – top right
        Box(
            modifier = Modifier
                .offset(
                    x = (80 + float1 * 30).dp,
                    y = (-50 + float1 * 40).dp
                )
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )
        // Medium circle – bottom left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(
                    x = (-40 + float2 * 20).dp,
                    y = (80 + float2 * 30).dp
                )
                .size(160.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.09f))
        )
        // Small circle – upper center
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = (-100 + float3 * 25).dp,
                    y = (60 + float3 * 15).dp
                )
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
        )
        // Tiny circle – mid right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(
                    x = (20 + float1 * 10).dp,
                    y = (40 + float2 * 20).dp
                )
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
        )
        // Another decorative circle
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(
                    x = (-30 + float3 * 15).dp,
                    y = (-20 + float1 * 25).dp
                )
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )

        // ── Page content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(
                        BorderStroke(2.dp, Color.White.copy(alpha = 0.3f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (pageIndex) {
                    0 -> Icons.Default.ChatBubble
                    1 -> Icons.Default.People
                    else -> Icons.Default.Favorite
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = page.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  LOGIN SCREEN  (toggles to SIGN UP mode)
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    var isLoginMode by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var selectedGender by rememberSaveable { mutableStateOf("") }
    var profilePictureUri by remember { mutableStateOf<Uri?>(null) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseManager.auth
    val themeColors = LocalThemeColors.current

    // ── Photo picker launcher ──
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        profilePictureUri = uri
    }

    // ── Date picker dialog ──
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Calendar.getInstance().apply {
            add(Calendar.YEAR, -18)
        }.timeInMillis
    )
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dob = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Google Sign-In via CredentialManager ──
    fun signInWithGoogle() {
        isLoading = true
        errorMessage = ""
        val credentialManager = CredentialManager.create(context)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context as Activity
                )
                val credentialData = result.credential.data
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(credentialData)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            val user = task.result?.user
                            if (user != null) {
                                FirebaseManager.setPresence(user.uid)
                                coroutineScope.launch {
                                    try {
                                        val token = com.google.firebase.messaging
                                            .FirebaseMessaging.getInstance().token.await()
                                        FirebaseManager.updateFcmToken(user.uid, token)
                                    } catch (_: Exception) {}
                                }
                            }
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            errorMessage =
                                "Google sign-in failed: ${task.exception?.message}"
                        }
                    }
            } catch (e: GoogleIdTokenParsingException) {
                isLoading = false
                errorMessage = "Invalid Google credential: ${e.message}"
                Log.w("LoginScreen", "GoogleIdTokenParsingException", e)
            } catch (e: GetCredentialException) {
                isLoading = false
                if (e is androidx.credentials.exceptions.NoCredentialException ||
                    e.message?.contains("cancel", ignoreCase = true) == true
                ) {
                    // User cancelled
                } else {
                    errorMessage = "Credential error: ${e.message}"
                    Log.w("LoginScreen", "GetCredentialException", e)
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Sign-in error: ${e.message}"
                Log.w("LoginScreen", "GoogleSignIn error", e)
            }
        }
    }

    // ── Email login / signup ──
    fun authenticate() {
        when {
            email.isBlank() -> {
                errorMessage = "Please enter your email"
                return
            }
            password.isBlank() || password.length < 6 -> {
                errorMessage = "Password must be at least 6 characters"
                return
            }
            !isLoginMode && name.isBlank() -> {
                errorMessage = "Please enter your name"
                return
            }
            !isLoginMode && dob.isBlank() -> {
                errorMessage = "Please select your date of birth"
                return
            }
            !isLoginMode && selectedGender.isBlank() -> {
                errorMessage = "Please select your gender"
                return
            }
        }

        isLoading = true
        errorMessage = ""

        if (isLoginMode) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null) {
                            FirebaseManager.setPresence(user.uid)
                            coroutineScope.launch {
                                try {
                                    val token = com.google.firebase.messaging
                                        .FirebaseMessaging.getInstance().token.await()
                                    FirebaseManager.updateFcmToken(user.uid, token)
                                } catch (_: Exception) {}
                            }
                        }
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        errorMessage = "Login failed: ${task.exception?.message}"
                    }
                }
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid ?: return@addOnCompleteListener
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        auth.currentUser?.updateProfile(profileUpdates)

                        coroutineScope.launch {
                            try {
                                var profileUrl = ""
                                if (profilePictureUri != null) {
                                    val inputStream =
                                        context.contentResolver.openInputStream(
                                            profilePictureUri!!
                                        )
                                    val bytes = inputStream?.readBytes()
                                    if (bytes != null) {
                                        profileUrl = SupabaseManager.uploadFile(
                                            "profiles",
                                            "${uid}.jpg",
                                            bytes
                                        )
                                    }
                                }

                                val username = name.lowercase().replace(" ", "") +
                                        "_" + (1000..9999).random()
                                val userProfile = UserProfile(
                                    uid = uid,
                                    name = name,
                                    username = username,
                                    dob = dob,
                                    gender = selectedGender,
                                    profilePicture = profileUrl
                                )
                                FirebaseManager.firestore
                                    .collection("users")
                                    .document(uid)
                                    .set(userProfile)
                                    .await()

                                FirebaseManager.setPresence(uid)
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Account created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Failed to save profile: ${e.message}"
                            }
                        }
                    } else {
                        isLoading = false
                        errorMessage = "Sign up failed: ${task.exception?.message}"
                    }
                }
        }
    }

    // ══════════════  UI  ══════════════

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B9D),
                        Color(0xFFE040FB),
                        Color(0xFFFF4081)
                    )
                )
            )
    ) {
        // ── Decorative background circles ──
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-80).dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 60.dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = (100).dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )

        // ── Scrollable main content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── App title ──
            Text(
                text = "EB Chat",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLoginMode) "Welcome back!" else "Create your account",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Glass morphism card ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        BorderStroke(1.dp, themeColors.glassBorder),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card title
                Text(
                    text = if (isLoginMode) "Sign In" else "Sign Up",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLoginMode) "Enter your credentials to continue"
                    else "Fill in the details to join EB Chat",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Profile picture (sign-up only) ──
                AnimatedVisibility(
                    visible = !isLoginMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                )
                                .border(
                                    BorderStroke(2.dp, themeColors.glassBorder),
                                    CircleShape
                                )
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePictureUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(profilePictureUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile picture",
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Add photo",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // Camera overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(
                                        BorderStroke(2.dp, Color.White),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Change photo",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        Text(
                            text = "Add Profile Photo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // ── Name field (sign-up only) ──
                AnimatedVisibility(
                    visible = !isLoginMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── Date of Birth field (sign-up only) ──
                AnimatedVisibility(
                    visible = !isLoginMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (dob.isEmpty()) "" else dob,
                            onValueChange = {},
                            label = { Text("Date of Birth") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            readOnly = true,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── Gender selection (sign-up only) ──
                AnimatedVisibility(
                    visible = !isLoginMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Gender",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Male", "Female", "Other").forEach { gender ->
                                FilterChip(
                                    selected = selectedGender == gender,
                                    onClick = { selectedGender = gender },
                                    label = {
                                        Text(
                                            gender,
                                            fontSize = 13.sp,
                                            fontWeight = if (selectedGender == gender)
                                                FontWeight.SemiBold
                                            else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selectedGender == gender)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── Email field ──
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Password field ──
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password"
                                else "Show password",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                // Forgot password link (login only)
                if (isLoginMode) {
                    TextButton(
                        onClick = { navController.navigate("forgot_password") },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Error message
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Login / Sign Up button ──
                Button(
                    onClick = { authenticate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (isLoginMode) "Login" else "Sign Up",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Divider with "or" ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "  or  ",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Google Sign-In / Sign-Up button ──
                OutlinedButton(
                    onClick = { signInWithGoogle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    // Google "G" icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isLoginMode) "Login with Google"
                        else "Sign up with Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Toggle login / sign-up ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? "
                        else "Already have an account? ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        text = if (isLoginMode) "Sign Up" else "Login",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            isLoginMode = !isLoginMode
                            errorMessage = ""
                        }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  FORGOT PASSWORD SCREEN
// ══════════════════════════════════════════════════════════════════

@Composable
fun ForgotPasswordScreen(navController: NavHostController) {
    var email by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var successMessage by rememberSaveable { mutableStateOf("") }

    val auth = FirebaseManager.auth
    val themeColors = LocalThemeColors.current

    fun sendResetEmail() {
        if (email.isBlank()) {
            errorMessage = "Please enter your email address"
            return
        }
        isLoading = true
        errorMessage = ""
        successMessage = ""

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    successMessage = "Password reset link sent to $email"
                } else {
                    errorMessage =
                        "Failed to send reset email: ${task.exception?.message}"
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B9D),
                        Color(0xFFE040FB)
                    )
                )
            )
    ) {
        // ── Decorative circles ──
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = (-70).dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 50.dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-60).dp, y = (120).dp)
                .size(90.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )

        // ── Glass card ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        BorderStroke(1.dp, themeColors.glassBorder),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                        .border(
                            BorderStroke(2.dp, themeColors.glassBorder),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Forgot Password?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your email address and we'll send you a link to reset your password.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Error message
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Success message
                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = Color(0xFF4CAF50),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Send Reset Link button
                Button(
                    onClick = { sendResetEmail() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Send Reset Link",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Back to Login
                TextButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Text(
                        text = "Back to Login",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  UTILITY FUNCTIONS
// ══════════════════════════════════════════════════════════════════

fun formatTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

fun formatDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}