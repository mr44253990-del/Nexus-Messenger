package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val isDarkTheme by PreferenceManager.isDarkTheme(context).collectAsState(initial = true)
      
      MyApplicationTheme(darkTheme = isDarkTheme) {
        EBChatApp()
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EBChatApp() {
  val context = LocalContext.current
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: "splash"

  val onboardingCompleted by PreferenceManager.isOnboardingCompleted(context).collectAsState(initial = null)
  
  if (onboardingCompleted == null) return // Wait for datastore

  val auth = remember { FirebaseAuth.getInstance() }
  val currentUser = auth.currentUser
  
  LaunchedEffect(currentUser) {
    if (currentUser != null) {
      try {
        val token = FirebaseMessaging.getInstance().token.await()
        FirebaseManager.updateFcmToken(currentUser.uid, token)
        
        // Subscribe to a generic topic for all users if needed
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
  
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) {
      if (!notificationPermission.status.isGranted) {
        notificationPermission.launchPermissionRequest()
      }
      if (!recordAudioPermission.status.isGranted) {
        recordAudioPermission.launchPermissionRequest()
      }
    }
  } else {
    LaunchedEffect(Unit) {
      if (!recordAudioPermission.status.isGranted) {
        recordAudioPermission.launchPermissionRequest()
      }
    }
  }

  val startDestination = when {
      onboardingCompleted == false -> "onboarding"
      auth.currentUser != null -> "home"
      else -> "login"
  }

  val showBottomBar = currentRoute in listOf("home", "chats", "community", "notifications", "settings")

  Scaffold(
    bottomBar = {
      if (showBottomBar) {
        NavigationBar {
          NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") { popUpTo(0) } }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Filled.Chat, contentDescription = "Chats") },
            label = { Text("Chats") },
            selected = currentRoute == "chats",
            onClick = { navController.navigate("chats") { popUpTo(0) } }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Filled.Group, contentDescription = "Community") },
            label = { Text("Community") },
            selected = currentRoute == "community",
            onClick = { navController.navigate("community") { popUpTo(0) } }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Filled.Notifications, contentDescription = "Notifications") },
            label = { Text("Alerts") },
            selected = currentRoute == "notifications",
            onClick = { navController.navigate("notifications") { popUpTo(0) } }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") { popUpTo(0) } }
          )
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = Modifier.padding(innerPadding)
    ) {
      composable("onboarding") { OnboardingScreen(navController) }
      composable("login") { LoginScreen(navController) }
      composable("home") { HomeFeedScreen(navController) }
      composable("notifications") { NotificationsScreen(navController) }
      composable("forgot_password") { ForgotPasswordScreen(navController) }
      composable("chats") { ChatsScreen(navController) }
      composable("community") { CommunityScreen(navController) }
      composable("group_detail/{groupId}/{groupName}") { backStackEntry ->
          val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
          val groupName = backStackEntry.arguments?.getString("groupName") ?: ""
          GroupChatDetailScreen(navController, groupId, groupName)
      }
      composable("settings") { SettingsScreen(navController) }
      composable("chat_detail/{targetUserId}/{targetUserName}") { backStackEntry ->
          val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
          val targetUserName = backStackEntry.arguments?.getString("targetUserName") ?: ""
          ChatDetailScreen(navController, targetUserId, targetUserName)
      }
    }
  }
}

@Composable
fun OnboardingScreen(navController: NavHostController) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var currentPage by remember { mutableStateOf(0) }
  
  val pages = listOf(
    OnboardingPage("Welcome to EB Chat", "Connect with anyone, anywhere in the world.", Color(0xFF6200EE)),
    OnboardingPage("Secure Messaging", "Your messages are protected with end-to-end encryption.", Color(0xFF03DAC5)),
    OnboardingPage("Join Communities", "Discover groups that share your interests.", Color(0xFFBB86FC))
  )

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier.size(200.dp).clip(CircleShape).background(pages[currentPage].color.copy(alpha = 0.2f)),
      contentAlignment = Alignment.Center
    ) {
      Text(pages[currentPage].title.take(1), fontSize = 80.sp, fontWeight = FontWeight.Bold, color = pages[currentPage].color)
    }
    Spacer(modifier = Modifier.height(48.dp))
    Text(pages[currentPage].title, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Spacer(modifier = Modifier.height(16.dp))
    Text(pages[currentPage].description, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    
    Spacer(modifier = Modifier.weight(1f))
    
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
      pages.forEachIndexed { index, _ ->
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (index == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Button(
      onClick = {
        if (currentPage < pages.size - 1) {
          currentPage++
        } else {
          coroutineScope.launch {
            PreferenceManager.setOnboardingCompleted(context)
            navController.navigate("login") { popUpTo("onboarding") { inclusive = true } }
          }
        }
      },
      modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
      Text(if (currentPage == pages.size - 1) "Get Started" else "Next")
    }
  }
}

data class OnboardingPage(val title: String, val description: String, val color: Color)


private fun webClientId(context: android.content.Context): String {
  val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
  return if (resourceId != 0) context.getString(resourceId) else "404800892817-4ovh3uvpn9mhee3kgedub3sr1k00udg1.apps.googleusercontent.com"
}

private suspend fun signInWithGoogleCredential(context: android.content.Context): com.google.firebase.auth.AuthResult {
  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId(webClientId(context))
    .build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()
  val credential = CredentialManager.create(context).getCredential(context, request).credential
  if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    throw IllegalStateException("Google did not return a valid ID token credential")
  }
  val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
  val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
  return FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
}

private suspend fun ensureGoogleUserProfile(user: com.google.firebase.auth.FirebaseUser) {
  val doc = FirebaseManager.firestore.collection("users").document(user.uid).get().await()
  if (!doc.exists()) {
    val displayName = user.displayName ?: user.email?.substringBefore('@') ?: "Nexus User"
    val profile = UserProfile(
      uid = user.uid,
      name = displayName,
      username = displayName.lowercase().replace(" ", "") + "_" + (1000..9999).random(),
      profilePicture = user.photoUrl?.toString() ?: ""
    )
    FirebaseManager.firestore.collection("users").document(user.uid).set(profile).await()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
  var isLoginMode by remember { mutableStateOf(true) }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var dob by remember { mutableStateOf("") }
  var gender by remember { mutableStateOf("") }
  var profilePicUri by remember { mutableStateOf<android.net.Uri?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  
  val auth = remember { FirebaseAuth.getInstance() }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text("EB Chat", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(8.dp))
    Text(if (isLoginMode) "Connect with friends & community" else "Create a new account", color = MaterialTheme.colorScheme.onSurfaceVariant)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    if (!isLoginMode) {
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri -> profilePicUri = uri }

        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (profilePicUri != null) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(profilePicUri),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Add Photo", modifier = Modifier.size(48.dp))
            }
        }
        Text("Upload Photo", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
    }
    if (errorMessage != null) {
      Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
      Spacer(modifier = Modifier.height(8.dp))
    }

    if (!isLoginMode) {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Name") },
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(8.dp))
      
      var showDatePicker by remember { mutableStateOf(false) }
      if (showDatePicker) {
          val datePickerState = rememberDatePickerState()
          DatePickerDialog(
              onDismissRequest = { showDatePicker = false },
              confirmButton = {
                  TextButton(onClick = {
                      datePickerState.selectedDateMillis?.let {
                          val date = java.util.Date(it)
                          val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                          dob = format.format(date)
                      }
                      showDatePicker = false
                  }) { Text("OK") }
              }
          ) {
              DatePicker(state = datePickerState)
          }
      }

      OutlinedTextField(
        value = dob,
        onValueChange = { },
        readOnly = true,
        label = { Text("Date of Birth") },
        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
      )
      Spacer(modifier = Modifier.height(8.dp))
      
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
          Text("Male")
          Spacer(modifier = Modifier.width(16.dp))
          RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
          Text("Female")
      }
      Spacer(modifier = Modifier.height(8.dp))
    }

    OutlinedTextField(
      value = email,
      onValueChange = { email = it },
      label = { Text("Email") },
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = password,
      onValueChange = { password = it },
      label = { Text("Password") },
      visualTransformation = PasswordVisualTransformation(),
      modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
      onClick = {
        if (email.isEmpty() || password.isEmpty()) {
           errorMessage = "Please fill out all fields"
           return@Button
        }
        if (!isLoginMode && (name.isEmpty() || dob.isEmpty() || gender.isEmpty())) {
           errorMessage = "Please fill out all fields"
           return@Button
        }
        isLoading = true
        errorMessage = null
        if (isLoginMode) {
          auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
              isLoading = false
              if (task.isSuccessful) {
                FirebaseManager.setPresence(task.result.user!!.uid)
                navController.navigate("home") { popUpTo("login") { inclusive = true } }
              } else {
                errorMessage = task.exception?.message ?: "Login failed"
              }
            }
        } else {
          auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
              if (task.isSuccessful) {
                val uid = task.result.user!!.uid
                coroutineScope.launch {
                    try {
                        var profileUrl = ""
                        if (profilePicUri != null) {
                            val inputStream = context.contentResolver.openInputStream(profilePicUri!!)
                            val bytes = inputStream?.readBytes()
                            if (bytes != null) {
                                profileUrl = SupabaseManager.uploadFile("profiles", "${uid}.jpg", bytes)
                            }
                        }
                        
                        val username = name.lowercase().replace(" ", "") + "_" + (1000..9999).random()
                        val userProfile = UserProfile(
                            uid = uid,
                            name = name,
                            username = username,
                            dob = dob,
                            gender = gender,
                            profilePicture = profileUrl
                        )
                        FirebaseManager.firestore.collection("users").document(uid).set(userProfile).await()
                        
                        FirebaseManager.setPresence(uid)
                        isLoading = false
                        Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Failed to save profile: ${e.message}"
                    }
                }
              } else {
                isLoading = false
                errorMessage = task.exception?.message ?: "Signup failed"
              }
            }
        }
      },
      modifier = Modifier.fillMaxWidth().height(50.dp),
      enabled = !isLoading
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
      } else {
        Text(if (isLoginMode) "Login" else "Sign Up")
      }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
      onClick = {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
          try {
            val result = signInWithGoogleCredential(context)
            val user = result.user ?: throw IllegalStateException("Google sign-in returned no Firebase user")
            ensureGoogleUserProfile(user)
            FirebaseManager.setPresence(user.uid)
            isLoading = false
            navController.navigate("home") { popUpTo("login") { inclusive = true } }
          } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message ?: "Google sign-in failed"
          }
        }
      },
      modifier = Modifier.fillMaxWidth().height(48.dp),
      enabled = !isLoading
    ) {
      Icon(Icons.Filled.AccountCircle, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Continue with Google")
    }
    Spacer(modifier = Modifier.height(16.dp))
    
    TextButton(onClick = { 
      navController.navigate("forgot_password")
    }) {
      Text("Forgot Password?")
    }
    
    Spacer(modifier = Modifier.height(8.dp))

    TextButton(onClick = { 
      isLoginMode = !isLoginMode 
      errorMessage = null
    }) {
      Text(if (isLoginMode) "Don't have an account? Sign up" else "Already have an account? Login")
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavHostController) {
  var email by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }
  
  val auth = remember { FirebaseAuth.getInstance() }

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text("Reset Password", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))
    Text("Enter your email address and we'll send you a link to reset your password.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    if (message != null) {
      Text(message!!, color = if (isError) MaterialTheme.colorScheme.error else Color.Green, fontSize = 14.sp)
      Spacer(modifier = Modifier.height(16.dp))
    }

    OutlinedTextField(
      value = email,
      onValueChange = { email = it },
      label = { Text("Email") },
      modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
      onClick = {
        if (email.isEmpty()) return@Button
        isLoading = true
        auth.sendPasswordResetEmail(email)
          .addOnCompleteListener { task ->
            isLoading = false
            if (task.isSuccessful) {
              message = "Reset link sent to your email!"
              isError = false
            } else {
              message = task.exception?.message ?: "Failed to send reset link"
              isError = true
            }
          }
      },
      modifier = Modifier.fillMaxWidth().height(50.dp),
      enabled = !isLoading
    ) {
      if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
      else Text("Send Reset Link")
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    TextButton(onClick = { navController.navigateUp() }) {
      Text("Back to Login")
    }
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(navController: NavHostController) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
  val posts by FirebaseManager.getSocialPosts().collectAsState(initial = emptyList())
  var feedNotice by remember { mutableStateOf<String?>(null) }
  var todoItems by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
  var supabaseStatus by remember { mutableStateOf("Connecting to Supabase...") }

  LaunchedEffect(Unit) {
    runCatching { withContext(Dispatchers.IO) { SupabaseManager.fetchTodoItems() } }
      .onSuccess { items ->
        todoItems = items
        supabaseStatus = if (items.isEmpty()) "Supabase ready • todos table is empty" else "Supabase ready • ${items.size} live rows"
      }
      .onFailure { error -> supabaseStatus = "Supabase connected for uploads • todos read unavailable: ${error.message ?: "check table/RLS"}" }
  }
  var showComposer by remember { mutableStateOf(false) }
  var title by remember { mutableStateOf("") }
  var body by remember { mutableStateOf("") }
  var tags by remember { mutableStateOf("") }
  var mediaUri by remember { mutableStateOf<Uri?>(null) }
  val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { mediaUri = it }

  if (showComposer) {
    AlertDialog(onDismissRequest = { showComposer = false }, title = { Text("Create social post") }, text = {
      Column {
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { body = it }, label = { Text("What is happening?") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedTextField(tags, { tags = it }, label = { Text("Tags: friends, update") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { mediaPicker.launch("image/*") }) { Text(if (mediaUri == null) "Attach photo/video" else "Media selected") }
      }
    }, confirmButton = {
      Button(onClick = {
        scope.launch {
          runCatching {
            var url = ""
            mediaUri?.let { uri ->
              val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
              if (bytes != null) url = SupabaseManager.uploadFile("posts", "${UUID.randomUUID()}.jpg", bytes)
            }
            FirebaseManager.createPost(currentUserId, title, body, tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }, url, "image")
          }.onSuccess {
            title = ""; body = ""; tags = ""; mediaUri = null; showComposer = false
            feedNotice = "Posted successfully"
          }.onFailure { error -> feedNotice = error.message ?: "Post failed. Check Supabase/Firebase config." }
        }
      }) { Text("Post") }
    }, dismissButton = { TextButton(onClick = { showComposer = false }) { Text("Cancel") } })
  }

  Scaffold(topBar = { TopAppBar(title = { Text("Nexus Social") }) }, floatingActionButton = { FloatingActionButton(onClick = { showComposer = true }) { Icon(Icons.Filled.Add, null) } }) { padding ->
    LazyColumn(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFE3F3), Color(0xFFFFF7FB), Color(0xFFEDE7FF)))).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      item {
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.70f))) {
          Column(Modifier.padding(16.dp)) {
            Text("Nexus Social", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFFC2185B))
            Text("Share posts, photos, tags and reactions. Chats are now one tap away in the bottom bar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Surface(
              color = Color(0xFFFFEBF6).copy(alpha = 0.82f),
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(Modifier.padding(12.dp)) {
                Text(supabaseStatus, fontWeight = FontWeight.SemiBold, color = Color(0xFFAD1457))
                if (todoItems.isNotEmpty()) {
                  Text(todoItems.take(3).joinToString(" • ") { it.name }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                  Text("Post media uploads still use your Supabase Storage buckets.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
        if (feedNotice != null) Text(feedNotice!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
      }
      items(posts.size) { index ->
        val post = posts[index]
        LaunchedEffect(post.postId) { try { FirebaseManager.incrementPostView(post.postId) } catch (e: Exception) { feedNotice = e.message } }
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.76f)), modifier = Modifier.fillMaxWidth()) {
          Column(Modifier.padding(16.dp)) {
            Text(post.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (post.body.isNotBlank()) Text(post.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (post.mediaUrl.isNotBlank()) coil.compose.AsyncImage(model = post.mediaUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
              listOf("❤️", "😂", "🔥", "👏").forEach { emoji -> AssistChip(onClick = { FirebaseManager.reactToPost(post.postId, currentUserId, emoji) }, label = { Text(emoji) }) }
            }
            Text("${post.views} views • ${post.reactions.size} reactions • ${post.tags.joinToString(" #", prefix = "#")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavHostController) {
  val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
  val notifications by FirebaseManager.getNotifications(uid).collectAsState(initial = emptyList())
  Scaffold(topBar = { TopAppBar(title = { Text("Notification Box") }) }) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding)) {
      if (notifications.isEmpty()) item { Text("No activity yet", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
      items(notifications.size) { i ->
        val n = notifications[i]
        ListItem(headlineContent = { Text(n.title) }, supportingContent = { Text(n.body) }, leadingContent = { Icon(Icons.Filled.Notifications, null, tint = if (n.read) MaterialTheme.colorScheme.outline else Color(0xFFE91E63)) })
        Divider()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavHostController) {
  val auth = remember { FirebaseAuth.getInstance() }
  val currentUserId = auth.currentUser?.uid
  
  var searchQuery by remember { mutableStateOf("") }
  var isSearching by remember { mutableStateOf(false) }

  val users by FirebaseManager.getAllUsersFlow().collectAsState(initial = emptyList())
  val stories by FirebaseManager.getStories().collectAsState(initial = emptyList())
  
  val filteredUsers = users.filter { 
      it.uid != currentUserId && 
      (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true))
  }
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { 
          if (isSearching) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search users...") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().height(50.dp)
            )
          } else {
            Text("Chats") 
          }
        },
        actions = {
          IconButton(onClick = { 
            isSearching = !isSearching 
            if (!isSearching) searchQuery = ""
          }) {
            Icon(
              if (isSearching) Icons.Filled.Close else Icons.Filled.Search,
              contentDescription = "Search"
            )
          }
          IconButton(onClick = {
            auth.signOut()
            navController.navigate("login") { popUpTo(0) }
          }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") }
        }
      )
    }
  ) { padding ->
    if (filteredUsers.isEmpty() && stories.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("No chats or stories found", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        item {
          StoriesBar(stories, users)
          Divider()
        }
        items(filteredUsers.size) { index ->
          val user = filteredUsers[index]
          ChatListItem(
            user = user,
            onClick = { navController.navigate("chat_detail/${user.uid}/${user.name}") }
          )
        }
      }
    }
  }
}

@Composable
fun StoriesBar(stories: List<FirebaseManager.Story>, allUsers: List<UserProfile>) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val auth = remember { FirebaseAuth.getInstance() }
  
  val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
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

  LazyRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    item {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
        Box(
          modifier = Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { launcher.launch("image/*") },
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Filled.AccountCircle, contentDescription = "Add Story", modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("My Story", fontSize = 12.sp)
      }
    }
    
    val userStories = stories.groupBy { it.userId }
    userStories.forEach { (userId, userStoriesList) ->
      val user = allUsers.find { it.uid == userId }
      if (user != null) {
        item {
          StoryItem(user, userStoriesList.first())
        }
      }
    }
  }
}

@Composable
fun StoryItem(user: UserProfile, story: FirebaseManager.Story) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary, CircleShape)
        .padding(2.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .padding(2.dp),
      contentAlignment = Alignment.Center
    ) {
      if (user.profilePicture.isNotEmpty()) {
        androidx.compose.foundation.Image(
          painter = coil.compose.rememberAsyncImagePainter(user.profilePicture),
          contentDescription = null,
          modifier = Modifier.fillMaxSize().clip(CircleShape),
          contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
      } else {
        Box(
          modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
        }
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(user.name.split(" ").first(), fontSize = 12.sp)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListItem(user: UserProfile, onClick: () -> Unit) {
  val status by FirebaseManager.getUserOnlineStatus(user.uid).collectAsState(initial = Pair(false, 0L))
  val isOnline = status.first
  val lastSeen = status.second
  
  val statusText = if (isOnline) "Online" else if (lastSeen > 0) "Last seen ${formatTime(lastSeen)}" else "Offline"
  
  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    color = Color.Transparent
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.size(50.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(user.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        if (isOnline) {
          Box(
            modifier = Modifier
              .size(14.dp)
              .clip(CircleShape)
              .background(Color.Green)
              .align(Alignment.BottomEnd)
          )
        }
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text(statusText, fontSize = 12.sp, color = if (isOnline) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("@${user.username}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
      }
    }
  }
}

fun formatTime(timeMillis: Long): String {
    val diff = System.currentTimeMillis() - timeMillis
    return when {
        diff < 60000 -> "just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatDetailScreen(navController: NavHostController, targetUserId: String, targetUserName: String) {
  var message by remember { mutableStateOf("") }
  var repliedToMessage by remember { mutableStateOf<FirebaseManager.Message?>(null) }
  
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val auth = remember { FirebaseAuth.getInstance() }
  val currentUserId = auth.currentUser?.uid ?: return
  val chatId = remember(currentUserId, targetUserId) { FirebaseManager.getChatId(currentUserId, targetUserId) }
  
  val messages by FirebaseManager.getMessages(chatId).collectAsState(initial = emptyList())
  val isTargetTyping by FirebaseManager.getTypingStatus(targetUserId, currentUserId).collectAsState(initial = false)
  val status by FirebaseManager.getUserOnlineStatus(targetUserId).collectAsState(initial = Pair(false, 0L))
  val isTargetOnline = status.first
  val lastSeen = status.second
  
  val statusText = if (isTargetTyping) "typing..." else if (isTargetOnline) "Online" else if (lastSeen > 0) "Last seen ${formatTime(lastSeen)}" else "Offline"
  
  val recorder = remember { VoiceRecorder(context) }
  var isRecording by remember { mutableStateOf(false) }
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
            val url = SupabaseManager.uploadFile("images", "${UUID.randomUUID()}.jpg", bytes)
            FirebaseManager.sendMessage(chatId, currentUserId, targetUserId, "", imageUrl = url)
          }
        } catch (e: Exception) {
          Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { 
          Column {
            Text(targetUserName, fontSize = 18.sp)
            Text(statusText, fontSize = 12.sp, color = if (isTargetTyping || isTargetOnline) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }) {
             Text("<", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(8.dp))
          }
        }
      )
    },
    bottomBar = {
      Column {
        if (repliedToMessage != null) {
            Surface(modifier = Modifier.fillMaxWidth().padding(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Replying to", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        Text(repliedToMessage!!.text, maxLines = 1, fontSize = 12.sp)
                    }
                    IconButton(onClick = { repliedToMessage = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel Reply", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        
        Row(
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = { 
            imagePickerLauncher.launch("image/*")
          }) {
            Icon(Icons.Filled.Add, contentDescription = "Add Image")
          }
          
          OutlinedTextField(
            value = message,
            onValueChange = { 
              message = it 
              FirebaseManager.setTypingStatus(currentUserId, targetUserId, it.isNotEmpty())
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message") },
            shape = RoundedCornerShape(24.dp)
          )
          
          Spacer(modifier = Modifier.width(8.dp))
          
          if (message.isBlank() && !isRecording) {
            IconButton(onClick = { 
                if (recordAudioPermission.status.isGranted) {
                    try {
                        isRecording = true
                        recorder.startRecording()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Recorder error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    recordAudioPermission.launchPermissionRequest()
                }
            }) {
              Icon(Icons.Filled.Mic, contentDescription = "Voice Message")
            }
          } else if (isRecording) {
             Row(verticalAlignment = Alignment.CenterVertically) {
                 Text("Recording...", color = Color.Red, fontSize = 12.sp)
                 IconButton(onClick = { 
                     try {
                         isRecording = false
                         val file = recorder.stopRecording()
                         if (file != null) {
                             coroutineScope.launch {
                                 try {
                                     val url = SupabaseManager.uploadFile("voices", "${UUID.randomUUID()}.mp3", file.readBytes())
                                     FirebaseManager.sendMessage(chatId, currentUserId, targetUserId, "", voiceUrl = url)
                                 } catch (e: Exception) {
                                     Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                 }
                             }
                         }
                     } catch (e: Exception) {
                         Toast.makeText(context, "Recorder error: ${e.message}", Toast.LENGTH_SHORT).show()
                     }
                 }) {
                   Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice")
                 }
                 IconButton(onClick = { 
                     isRecording = false
                     recorder.cancelRecording()
                 }) {
                   Icon(Icons.Filled.Delete, contentDescription = "Cancel Voice")
                 }
             }
          } else {
            FloatingActionButton(
              onClick = { 
                if (message.isNotBlank()) {
                  FirebaseManager.sendMessage(
                    chatId, 
                    currentUserId, 
                    targetUserId, 
                    message.trim(),
                    repliedToId = repliedToMessage?.messageId,
                    repliedToText = repliedToMessage?.text
                  )
                  message = ""
                  repliedToMessage = null
                  FirebaseManager.setTypingStatus(currentUserId, targetUserId, false)
                }
              },
              modifier = Modifier.size(50.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
          }
        }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
      reverseLayout = true
    ) {
      items(messages.size) { index ->
        val msg = messages[index]
        MessageBubble(
            msg = msg, 
            isMe = msg.senderId == currentUserId,
            onReply = { repliedToMessage = it },
            onDelete = { FirebaseManager.deleteMessage(chatId, msg.messageId) }
        )
      }
    }
  }
}

@Composable
fun MessageBubble(msg: FirebaseManager.Message, isMe: Boolean, onReply: (FirebaseManager.Message) -> Unit, onDelete: () -> Unit) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bgColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    var showMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), 
        horizontalAlignment = alignment
    ) {
        if (msg.repliedToText != null) {
            Surface(
                modifier = Modifier.padding(bottom = 2.dp).padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(msg.repliedToText, fontSize = 10.sp, modifier = Modifier.padding(4.dp), maxLines = 1)
            }
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .clickable { showMenu = true }
                .padding(12.dp)
        ) {
            Column {
                if (msg.voiceUrl != null) {
                    VoiceMessagePlayer(msg.voiceUrl, textColor)
                } else if (msg.imageUrl != null) {
                    coil.compose.AsyncImage(
                        model = msg.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(msg.text, color = textColor)
                }
                Text(
                    formatTime(msg.timestamp), 
                    fontSize = 10.sp, 
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
            
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Reply") },
                    onClick = { 
                        onReply(msg)
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
                if (isMe) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { 
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceMessagePlayer(voiceUrl: String, textColor: Color) {
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mediaPlayer = remember { android.media.MediaPlayer() }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            if (isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            } else {
                try {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(voiceUrl)
                    mediaPlayer.setOnErrorListener { _, _, _ ->
                        isPlaying = false
                        Toast.makeText(context, "Playback error", Toast.LENGTH_SHORT).show()
                        true
                    }
                    mediaPlayer.prepareAsync()
                    mediaPlayer.setOnPreparedListener {
                        it.start()
                        isPlaying = true
                    }
                    mediaPlayer.setOnCompletionListener {
                        isPlaying = false
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error playing: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }, modifier = Modifier.size(24.dp)) {
            Icon(
                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, 
                contentDescription = null, 
                tint = textColor
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("Voice Note", color = textColor, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavHostController) {
  val groups by FirebaseManager.getGroups().collectAsState(initial = emptyList())
  var showCreateDialog by remember { mutableStateOf(false) }
  val auth = remember { FirebaseAuth.getInstance() }
  val context = LocalContext.current
  
  if (showCreateDialog) {
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    
    AlertDialog(
      onDismissRequest = { showCreateDialog = false },
      title = { Text("Create New Community") },
      text = {
        Column {
          OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group Name") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = groupDesc,
            onValueChange = { groupDesc = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          if (groupName.isNotBlank() && auth.currentUser != null) {
            FirebaseManager.createGroup(groupName, groupDesc, auth.currentUser!!.uid)
            showCreateDialog = false
          }
        }) {
          Text("Create")
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
        title = { Text("Community & Groups") }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { showCreateDialog = true }) {
        Icon(Icons.Filled.Group, contentDescription = "Create Group")
      }
    }
  ) { padding ->
    if (groups.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("No groups available. Create one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        items(groups.size) { index ->
          val group = groups[index]
          ListItem(
            headlineContent = { Text(group.name, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(group.description, maxLines = 1) },
            leadingContent = {
              Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Text(group.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
              }
            },
            modifier = Modifier.clickable { 
                navController.navigate("group_detail/${group.groupId}/${group.name}")
            }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GroupChatDetailScreen(navController: NavHostController, groupId: String, groupName: String) {
    var message by remember { mutableStateOf("") }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid ?: return
    val messages by FirebaseManager.getGroupMessages(groupId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    
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
                        val url = SupabaseManager.uploadFile("group_images", "${UUID.randomUUID()}.jpg", bytes)
                        FirebaseManager.sendGroupMessage(groupId, currentUserId, "", imageUrl = url)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Back", modifier = Modifier.padding(8.dp))
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message group...") },
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Image")
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (message.isBlank() && !isRecording) {
                    IconButton(onClick = { 
                        if (recordAudioPermission.status.isGranted) {
                            try {
                                isRecording = true
                                recorder.startRecording()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Recorder error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            recordAudioPermission.launchPermissionRequest()
                        }
                    }) { Icon(Icons.Filled.Mic, contentDescription = "Voice") }
                } else if (isRecording) {
                    IconButton(onClick = { 
                        try {
                            isRecording = false
                            val file = recorder.stopRecording()
                            if (file != null) {
                                coroutineScope.launch {
                                    try {
                                        val url = SupabaseManager.uploadFile("group_voices", "${UUID.randomUUID()}.mp3", file.readBytes())
                                        FirebaseManager.sendGroupMessage(groupId, currentUserId, "", voiceUrl = url)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Recorder error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) { Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.Red) }
                } else {
                    IconButton(onClick = {
                        if (message.isNotBlank()) {
                            FirebaseManager.sendGroupMessage(groupId, currentUserId, message.trim())
                            message = ""
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            items(messages.size) { index ->
                val msg = messages[index]
                MessageBubble(
                    msg = msg, 
                    isMe = msg.senderId == currentUserId,
                    onReply = {}, 
                    onDelete = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userProfile by FirebaseManager.getCurrentUserProfile().collectAsState(initial = null)
  val isDarkTheme by PreferenceManager.isDarkTheme(context).collectAsState(initial = true)
  var editProfile by remember { mutableStateOf(false) }
  var editName by remember(userProfile?.name) { mutableStateOf(userProfile?.name ?: "") }
  var themeName by remember(userProfile?.themeName) { mutableStateOf(userProfile?.themeName ?: "Pink Glass") }
  var pickedProfileUri by remember { mutableStateOf<Uri?>(null) }
  val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { pickedProfileUri = it }

  if (editProfile && userProfile != null) {
    AlertDialog(onDismissRequest = { editProfile = false }, title = { Text("Edit profile") }, text = {
      Column {
        OutlinedTextField(editName, { editName = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(themeName, { themeName = it }, label = { Text("Theme name") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { profilePicker.launch("image/*") }) { Text(if (pickedProfileUri == null) "Upload new profile photo" else "New photo selected") }
      }
    }, confirmButton = {
      Button(onClick = {
        coroutineScope.launch {
          var photo = userProfile!!.profilePicture
          pickedProfileUri?.let { uri ->
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            if (bytes != null) photo = SupabaseManager.uploadFile("profiles", "${userProfile!!.uid}.jpg", bytes)
          }
          FirebaseManager.updateProfile(userProfile!!.uid, editName, photo, themeName)
          editProfile = false
        }
      }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = { editProfile = false }) { Text("Cancel") } })
  }
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings & Profile") }
      )
    }
  ) { padding ->
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
      item {
        if (userProfile != null) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              if (userProfile!!.profilePicture.isNotEmpty()) {
                androidx.compose.foundation.Image(
                  painter = coil.compose.rememberAsyncImagePainter(userProfile!!.profilePicture),
                  contentDescription = null,
                  modifier = Modifier.fillMaxSize().clip(CircleShape),
                  contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
              } else {
                Text(userProfile!!.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 36.sp)
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(userProfile!!.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("@${userProfile!!.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Text(userProfile!!.themeName, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { editProfile = true }) { Text("Edit Profile") }
            Spacer(modifier = Modifier.height(24.dp))
            
            ListItem(
              headlineContent = { Text("Email") },
              supportingContent = { Text(FirebaseAuth.getInstance().currentUser?.email ?: "") },
              leadingContent = { Icon(Icons.Filled.AccountCircle, contentDescription = null) }
            )
            ListItem(
              headlineContent = { Text("Date of Birth") },
              supportingContent = { Text(userProfile!!.dob) },
              leadingContent = { Icon(Icons.Filled.AccountCircle, contentDescription = null) }
            )
            ListItem(
              headlineContent = { Text("Gender") },
              supportingContent = { Text(userProfile!!.gender) },
              leadingContent = { Icon(Icons.Filled.AccountCircle, contentDescription = null) }
            )
          }
        }
      }
      
      item {
        Divider()
        ListItem(
          headlineContent = { Text("Pink Glass Theme") },
          supportingContent = { Text("Global pink, rose, purple and glass-style surfaces") },
          leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) }
        )
        ListItem(
          headlineContent = { Text("Mute / Block Controls") },
          supportingContent = { Text("Open a chat profile to mute, block, unblock or stop notifications") },
          leadingContent = { Icon(Icons.Filled.Block, contentDescription = null) }
        )
        ListItem(
          headlineContent = { Text("Notifications") },
          leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
          trailingContent = { Switch(checked = true, onCheckedChange = {}) }
        )
        ListItem(
          headlineContent = { Text("Dark Mode") },
          leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
          trailingContent = { 
            Switch(
              checked = isDarkTheme, 
              onCheckedChange = { 
                coroutineScope.launch { PreferenceManager.setDarkTheme(context, it) } 
              }
            ) 
          }
        )
        ListItem(
          headlineContent = { Text("Logout") },
          leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
          modifier = Modifier.clickable { 
              FirebaseAuth.getInstance().signOut()
              navController.navigate("login") { popUpTo(0) }
          }
        )
        ListItem(
          headlineContent = { Text("App Version") },
          supportingContent = { Text("1.0.0") },
          leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) }
        )
      }
    }
  }
}

