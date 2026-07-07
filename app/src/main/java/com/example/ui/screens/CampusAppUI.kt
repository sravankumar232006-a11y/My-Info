package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CampusViewModel
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.PaymentReceipt
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

enum class CampusScreen {
    Splash,
    Login,
    Dashboard
}

enum class DashboardTab {
    Home,
    Attendance,
    Academics,
    Payments,
    AiChat
}

// --- Main Navigation Entry Composable ---

@Composable
fun CampusAppUI(viewModel: CampusViewModel) {
    var currentScreen by remember { mutableStateOf(CampusScreen.Splash) }
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            currentScreen = CampusScreen.Dashboard
        }
    }

    Crossfade(targetState = currentScreen, animationSpec = tween(600), label = "ScreenTransition") { screen ->
        when (screen) {
            CampusScreen.Splash -> SplashScreen {
                currentScreen = if (isLoggedIn) CampusScreen.Dashboard else CampusScreen.Login
            }
            CampusScreen.Login -> LoginScreen(viewModel)
            CampusScreen.Dashboard -> MainDashboardContainer(viewModel)
        }
    }
}

// --- 1. SPLASH SCREEN ---

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        restartOnPlay = false
    )

    LaunchedEffect(progress) {
        if (progress == 1.0f) {
            onTimeout()
        }
    }

    // Safety timeout in case Lottie fails to load
    LaunchedEffect(Unit) {
        delay(3500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CampusBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize()
                )
                Image(
                    painter = painterResource(id = R.drawable.img_campus_logo),
                    contentDescription = "Campus ERP Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "CAMPUS ERP",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CampusPrimary,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your All-in-One Digital Campus Portal",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- 2. LOGIN SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: CampusViewModel) {
    var rollNumber by remember { mutableStateOf("IIT-2024-042") }
    var pin by remember { mutableStateOf("1234") }
    var rememberMe by remember { mutableStateOf(true) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showGoogleSelector by remember { mutableStateOf(false) }
    var showPhoneAuthFlow by remember { mutableStateOf(false) }
    var phoneStep by remember { mutableIntStateOf(1) } // 1: input, 2: OTP, 3: Register name
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CampusBackground),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_campus_logo),
                    contentDescription = "University Logo",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome Back",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Sign in to access your digital campus",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = rollNumber,
                    onValueChange = { rollNumber = it },
                    label = { Text("Student Roll Number") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("4-Digit Portal PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it }
                        )
                        Text("Remember Login", fontSize = 12.sp)
                    }
                    TextButton(onClick = { showHelpDialog = true }) {
                        Text("Need Help?", fontSize = 12.sp)
                    }
                }

                if (loginError != null) {
                    Text(
                        text = loginError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = { viewModel.loginUser(rollNumber, pin, rememberMe) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SIGN IN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Or Authenticate Securely",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Custom Google Sign-In button
                OutlinedButton(
                    onClick = { showGoogleSelector = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp).padding(end = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(Color(0xFFEA4335), 180f, 90f, true) // Red
                                drawArc(Color(0xFFFBBC05), 90f, 90f, true)  // Yellow
                                drawArc(Color(0xFF34A853), 0f, 90f, true)   // Green
                                drawArc(Color(0xFF4285F4), 270f, 90f, true) // Blue
                            }
                        }
                        Text(
                            text = "Continue with Google",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Auth button
                OutlinedButton(
                    onClick = {
                        showPhoneAuthFlow = true
                        phoneStep = 1
                        phoneNumber = ""
                        otpCode = ""
                        registerName = ""
                        registerEmail = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = CampusPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign in with Phone Number",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Scanning fingerprint...", Toast.LENGTH_SHORT).show()
                        viewModel.loginUser("IIT-2024-042", "1234", true)
                    }
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Instant Biometric Unlock",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showGoogleSelector) {
        Dialog(onDismissRequest = { showGoogleSelector = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(Color(0xFFEA4335), 180f, 90f, true)
                                drawArc(Color(0xFFFBBC05), 90f, 90f, true)
                                drawArc(Color(0xFF34A853), 0f, 90f, true)
                                drawArc(Color(0xFF4285F4), 270f, 90f, true)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Choose an account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "to continue to Campus ERP Sandbox",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    val googleAccounts = listOf(
                        Triple("Sravan Kumar", "sravankumar232006@gmail.com", "S"),
                        Triple("Aarav Sharma", "aarav.sharma@campus.edu", "A"),
                        Triple("Guest Student", "guest.student@gmail.com", "G")
                    )

                    googleAccounts.forEach { (name, email, initial) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleSelector = false
                                    Toast.makeText(context, "Signing in with $email...", Toast.LENGTH_SHORT).show()
                                    viewModel.loginWithCustomUser(name, email, "+1 (555) 234-5678")
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CampusPrimary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    fontWeight = FontWeight.Bold,
                                    color = CampusPrimary,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = email,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Divider(color = Color(0xFFF1F5F9))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Google Play Services integration simulation.", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp).padding(start = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = "Add another account",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To continue, Google will share your name, email address, language preference, and profile picture with Campus ERP.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showPhoneAuthFlow) {
        Dialog(onDismissRequest = { showPhoneAuthFlow = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Secure Phone Login",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = CampusPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    when (phoneStep) {
                        1 -> {
                            Text(
                                text = "Enter your mobile number to sign in or create a student portal account.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it.filter { char -> char.isDigit() } },
                                label = { Text("Mobile Number") },
                                placeholder = { Text("98765 43210") },
                                leadingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                                    ) {
                                        Text("+91", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Divider(modifier = Modifier.height(16.dp).width(1.dp), color = Color.LightGray)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (phoneNumber.length == 10) {
                                        generatedOtp = String.format("%04d", (1000..9999).random())
                                        phoneStep = 2
                                        Toast.makeText(context, "OTP Sent Successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please enter a valid 10-digit mobile number.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Send OTP Verification", fontWeight = FontWeight.Bold)
                            }
                        }
                        2 -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CampusPrimary.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CampusPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Simulated SMS OTP: $generatedOtp",
                                        fontWeight = FontWeight.Bold,
                                        color = CampusPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Text(
                                text = "Enter the 4-digit verification code sent to +91 $phoneNumber.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it.filter { char -> char.isDigit() }.take(4) },
                                label = { Text("Enter OTP Code") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("XXXX") }
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (otpCode == generatedOtp) {
                                        if (phoneNumber == "9876543210") {
                                            viewModel.loginWithCustomUser("Aarav Sharma", "aarav.sharma@campus.edu", "+91 $phoneNumber")
                                            showPhoneAuthFlow = false
                                        } else {
                                            phoneStep = 3
                                        }
                                    } else {
                                        Toast.makeText(context, "Invalid OTP code. Try $generatedOtp", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Verify & Continue", fontWeight = FontWeight.Bold)
                            }
                        }
                        3 -> {
                            Text(
                                text = "Register New Student Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Provide your details to complete ERP onboarding.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = registerName,
                                onValueChange = { registerName = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = registerEmail,
                                onValueChange = { registerEmail = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (registerName.isNotBlank() && registerEmail.isNotBlank()) {
                                        viewModel.loginWithCustomUser(
                                            name = registerName.trim(),
                                            email = registerEmail.trim(),
                                            phone = "+91 $phoneNumber"
                                        )
                                        showPhoneAuthFlow = false
                                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "All fields are required.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Complete Registration", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showPhoneAuthFlow = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Portal Credentials") },
            text = {
                Text(
                    "To access the student ERP sandbox environment, use the preset student credentials:\n\n" +
                    "Roll Number: IIT-2024-042\n" +
                    "PIN: 1234\n\n" +
                    "Or tap the Fingerprint scanner at the bottom to perform instant biometric unlock."
                )
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

// --- 3. MAIN DASHBOARD CONTAINER ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardContainer(viewModel: CampusViewModel) {
    var activeTab by remember { mutableStateOf(DashboardTab.Home) }
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            if (activeTab == DashboardTab.Home) {
                // Custom welcome header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(AccentIndigoGradientStart, AccentIndigoGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AS",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WELCOME BACK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Gray,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = profile?.name ?: "Aarav Sharma",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CampusPrimary
                            )
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Light/Dark mode manual override toggle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleDarkMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = CampusPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                                    .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable {
                                    Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = CampusPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.logout() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = AlertError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // Unified clean title top bar for other tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (activeTab) {
                                    DashboardTab.Attendance -> Icons.Default.CalendarMonth
                                    DashboardTab.Academics -> Icons.Default.MenuBook
                                    DashboardTab.Payments -> Icons.Default.Payments
                                    DashboardTab.AiChat -> Icons.Default.SmartToy
                                    else -> Icons.Default.School
                                },
                                contentDescription = null,
                                tint = CampusSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (activeTab) {
                                DashboardTab.Attendance -> "Attendance Tracker"
                                DashboardTab.Academics -> "Academics & Courses"
                                DashboardTab.Payments -> "Tuition & Fees"
                                DashboardTab.AiChat -> "Campus AI Assistant"
                                else -> "Campus ERP"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CampusPrimary
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Light/Dark mode manual override toggle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleDarkMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = CampusPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.logout() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = AlertError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == DashboardTab.Home,
                    onClick = { activeTab = DashboardTab.Home },
                    icon = { Icon(if (activeTab == DashboardTab.Home) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Home") },
                    label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CampusPrimary,
                        selectedTextColor = CampusPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = AccentBlueLight
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.Attendance,
                    onClick = { activeTab = DashboardTab.Attendance },
                    icon = { Icon(if (activeTab == DashboardTab.Attendance) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth, contentDescription = "Attendance") },
                    label = { Text("Attendance", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CampusPrimary,
                        selectedTextColor = CampusPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = AccentBlueLight
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.Academics,
                    onClick = { activeTab = DashboardTab.Academics },
                    icon = { Icon(if (activeTab == DashboardTab.Academics) Icons.Filled.MenuBook else Icons.Outlined.MenuBook, contentDescription = "Academics") },
                    label = { Text("Academics", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CampusPrimary,
                        selectedTextColor = CampusPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = AccentBlueLight
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.Payments,
                    onClick = { activeTab = DashboardTab.Payments },
                    icon = { Icon(if (activeTab == DashboardTab.Payments) Icons.Filled.Payments else Icons.Outlined.Payments, contentDescription = "Fees") },
                    label = { Text("Payments", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CampusPrimary,
                        selectedTextColor = CampusPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = AccentBlueLight
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.AiChat,
                    onClick = { activeTab = DashboardTab.AiChat },
                    icon = { Icon(if (activeTab == DashboardTab.AiChat) Icons.Filled.SmartToy else Icons.Outlined.SmartToy, contentDescription = "Campus AI") },
                    label = { Text("CampusAI", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CampusPrimary,
                        selectedTextColor = CampusPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = AccentBlueLight
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Crossfade(targetState = activeTab, label = "TabTransition") { tab ->
                when (tab) {
                    DashboardTab.Home -> TabHome(viewModel)
                    DashboardTab.Attendance -> TabAttendance(viewModel)
                    DashboardTab.Academics -> TabAcademics(viewModel)
                    DashboardTab.Payments -> TabPayments(viewModel)
                    DashboardTab.AiChat -> TabAiChat(viewModel)
                }
            }
        }
    }
}

// --- TAB 1: HOME (DASHBOARD) ---

@Composable
fun TabHome(viewModel: CampusViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val circulars by viewModel.campusCirculars.collectAsStateWithLifecycle()
    val subjects by viewModel.academicSubjects.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showQrDialog by remember { mutableStateOf(false) }
    var showTicketDialog by remember { mutableStateOf(false) }
    var showCircularDialog by remember { mutableStateOf<CircularEntity?>(null) }
    var showHostelDialog by remember { mutableStateOf(false) }
    var showTransportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Hero Profile Header Card styled as "Quick Insights"
        item {
            profile?.let { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CampusPrimary
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // Draw ambient blue glowing circles
                                drawCircle(
                                    color = Color(0x223B82F6),
                                    radius = 240f,
                                    center = Offset(size.width + 40f, -40f)
                                )
                            }
                            .padding(24.dp)
                    ) {
                        Column {
                            // Top Row: Attendance & Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "ATTENDANCE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF93C5FD),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = p.attendancePercentage.toString(),
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.Light,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "%",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF93C5FD),
                                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                                        )
                                    }
                                }
                                
                                // Status badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (p.attendancePercentage >= 75) "Good Standing" else "Ineligible",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Bottom Grid: CGPA & Branch info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // CGPA Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "CURRENT CGPA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF93C5FD).copy(alpha = 0.7f),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = p.cgpa.toString(),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "↑0.2",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF60A5FA)
                                            )
                                        }
                                    }
                                }
                                
                                // Semester Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "SEMESTER / BRANCH",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF93C5FD).copy(alpha = 0.7f),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Sem ${p.semester} • ${p.branch}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Dynamic Greeting & Weather
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val greeting = when (hour) {
                            in 0..11 -> "Good Morning ☀️"
                            in 12..16 -> "Good Afternoon 🌤️"
                            else -> "Good Evening 🌙"
                        }
                        Text(
                            text = "$greeting, Aarav",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CampusPrimary
                        )
                        Text(
                            text = "Bengaluru Campus Hub • Today is clear",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = "78°F",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CampusSecondary
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Today's Schedule (Upcoming Class Card)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Upcoming Class",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CampusPrimary
                )
                Text(
                    text = "View Timetable",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Explore schedule below", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Upcoming Class Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Toast.makeText(context, "Class details: Advanced Data Structures in Room 402B", Toast.LENGTH_SHORT).show()
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Box
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "10:30",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF4F46E5)
                            )
                            Text(
                                text = "AM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color(0xFF818CF8)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Class Details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Advanced Data Structures",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CampusOnSurface
                        )
                        Text(
                            text = "Prof. S. Iyer • Room 402B",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // Divider line
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(Color(0xFFF1F5F9))
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Map Pin Icon
                    Text(
                        text = "📍",
                        fontSize = 20.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Section Title: Quick Actions
        item {
            Text(
                text = "Quick Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CampusPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }

        // Quick actions Grid matching the Modules grid from mockup
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action 1: QR Check-In
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showQrDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ColorAcademics),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "QR Class In",
                            tint = Color(0xFF5B21B6),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "QR CHECK-IN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                // Action 2: Raise Ticket
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTicketDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ColorPayments),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = "Helpdesk",
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SUPPORT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                // Action 3: Hostel Menu
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showHostelDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ColorExams),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HomeWork,
                            contentDescription = "Hostel Hub",
                            tint = Color(0xFF991B1B),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HOSTEL HUB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                // Action 4: Shuttle Tracker
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTransportDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ColorTransport),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsBus,
                            contentDescription = "Bus Tracker",
                            tint = Color(0xFF075985),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SHUTTLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Section Title: Circular Feed
        item {
            Text(
                text = "Campus Circulars & Events",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CampusPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }

        // Circular feeds items with minimalist list cards
        items(circulars) { c ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { showCircularDialog = c },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when (c.category) {
                                    "Notice" -> ColorAcademics
                                    "Placement" -> ColorPayments
                                    "Event" -> ColorEvents
                                    else -> ColorTransport
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (c.category) {
                                "Notice" -> Icons.Default.Campaign
                                "Placement" -> Icons.Default.Work
                                "Event" -> Icons.Default.EventNote
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (c.category) {
                                "Notice" -> Color(0xFF5B21B6)
                                "Placement" -> Color(0xFF065F46)
                                "Event" -> Color(0xFF6B21A8)
                                else -> Color(0xFF075985)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(c.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(c.description, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${c.publisher} • Just Now",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CampusSecondary
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // --- Interactive Dialogue Sheets ---

    // 1. QR Class Check-In Simulation
    if (showQrDialog) {
        var selectedSubjectToScan by remember { mutableStateOf(subjects.firstOrNull()?.code ?: "") }
        var scanSuccessMsg by remember { mutableStateOf<String?>(null) }
        var isScanning by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showQrDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Scanned Class Check-In", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CampusPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (scanSuccessMsg == null) {
                        Text("Select course to simulate instructor's QR scan:", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subjects) { s ->
                                FilterChip(
                                    selected = selectedSubjectToScan == s.code,
                                    onClick = { selectedSubjectToScan = s.code },
                                    label = { Text(s.code, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isScanning) {
                            // Scanning animation bar
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CampusSecondary, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "scanning")
                                val scanOffset by infiniteTransition.animateFloat(
                                    initialValue = -80f,
                                    targetValue = 80f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "bar"
                                )
                                Divider(
                                    color = CampusSecondary,
                                    thickness = 3.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = scanOffset.dp)
                                )
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = CampusSecondary.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
                            }
                        } else {
                            Button(onClick = {
                                isScanning = true
                                scope.launch {
                                    delay(2000)
                                    val matchedSub = subjects.find { it.code == selectedSubjectToScan }
                                    if (matchedSub != null) {
                                        scanSuccessMsg = viewModel.markAttendanceViaQR("${matchedSub.code}|${matchedSub.name}")
                                    }
                                    isScanning = false
                                }
                            }) {
                                Text("Simulate QR Scan")
                            }
                        }
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AlertSuccess, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(scanSuccessMsg ?: "", fontSize = 13.sp, textAlign = TextAlign.Center, color = AlertSuccess)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showQrDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }

    // 2. Helpdesk Ticket Creator
    if (showTicketDialog) {
        var category by remember { mutableStateOf("Academic") }
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        val categories = listOf("Academic", "Hostel", "Transport", "Accounts", "Technical")

        Dialog(onDismissRequest = { showTicketDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("File a Support Ticket", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CampusPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Ticket Category", fontSize = 11.sp, modifier = Modifier.align(Alignment.Start))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Short Issue Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showTicketDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.trim().isNotEmpty() && description.trim().isNotEmpty()) {
                                    viewModel.raiseTicket(category, title, description)
                                    showTicketDialog = false
                                    Toast.makeText(context, "Ticket Raised Successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Raise Ticket")
                        }
                    }
                }
            }
        }
    }

    // 3. Circular Detail Dialog
    showCircularDialog?.let { c ->
        AlertDialog(
            onDismissRequest = { showCircularDialog = null },
            title = { Text(c.title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Category: ${c.category} • Published by: ${c.publisher}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CampusPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(c.description, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showCircularDialog = null }) {
                    Text("OK")
                }
            }
        )
    }

    // 4. Hostel Hub Dialog
    if (showHostelDialog) {
        Dialog(onDismissRequest = { showHostelDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Hostel Hub (Room 304)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CampusPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Current Block: Emerald Hostel, North Wing", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Today's Mess Menu:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Breakfast: Idli, Sambar, Tea", fontSize = 12.sp)
                    Text("• Lunch: Paneer Butter Masala, Roti, Dal, Rice", fontSize = 12.sp)
                    Text("• Dinner: Veg Biryani, Raita, Fruit Ice-cream", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showHostelDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }

    // 5. Shuttle Tracker dialog
    if (showTransportDialog) {
        val coords by viewModel.busCoordinates.collectAsStateWithLifecycle()
        Dialog(onDismissRequest = { showTransportDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Live Campus Shuttle Tracker", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CampusPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Route: Campus Main Gate ⇄ North Hostel Complex", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Draw static vector simulated mini-map on canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(CampusBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    ) {
                        // Drawing path lines
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(20f, 60f),
                            end = Offset(size.width - 20f, 60f),
                            strokeWidth = 8f
                        )
                        // Campus checkpoints
                        drawCircle(color = CampusPrimary, radius = 12f, center = Offset(40f, 60f))
                        drawCircle(color = CampusSecondary, radius = 12f, center = Offset(size.width / 2f, 60f))
                        drawCircle(color = CampusPrimary, radius = 12f, center = Offset(size.width - 40f, 60f))

                        // Dynamic animated bus coordinate position marker
                        val t = (System.currentTimeMillis() % 6000) / 6000f
                        val busX = 40f + (size.width - 80f) * t
                        drawCircle(color = CampusTertiary, radius = 16f, center = Offset(busX, 60f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Coordinates: Lat ${"%.5f".format(coords.first)}, Lng ${"%.5f".format(coords.second)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CampusPrimary)
                    Text("Estimated Arrival: 4 mins (Shuttle #3)", fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showTransportDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

// --- TAB 2: ATTENDANCE ---

@Composable
fun TabAttendance(viewModel: CampusViewModel) {
    val subjects by viewModel.academicSubjects.collectAsStateWithLifecycle()
    val logs by viewModel.attendanceLogs.collectAsStateWithLifecycle()
    var showLeaveDialog by remember { mutableStateOf<SubjectEntity?>(null) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Section: Analytics graph using custom Canvas drawing
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Subject Attendance Analytics",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = CampusPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw a custom Bar Chart for each subject with beautiful rounded rects
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val spacing = size.width / (subjects.size + 1)
                        subjects.forEachIndexed { index, sub ->
                            val xPos = spacing * (index + 1)
                            val attendanceRate = if (sub.totalClasses > 0) sub.attendanceCount.toFloat() / sub.totalClasses else 0f
                            val barHeight = size.height * 0.75f * attendanceRate

                            // Draw background bar track with rounded corner
                            drawRoundRect(
                                color = Color(0xFFF1F5F9),
                                topLeft = Offset(xPos - 12f, size.height * 0.1f),
                                size = androidx.compose.ui.geometry.Size(24f, size.height * 0.75f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                            )

                            // Draw filled attendance bar with rounded corner
                            if (barHeight > 0f) {
                                drawRoundRect(
                                    color = if (attendanceRate >= 0.75f) CampusSecondary else AlertWarning,
                                    topLeft = Offset(xPos - 12f, size.height * 0.85f - barHeight),
                                    size = androidx.compose.ui.geometry.Size(24f, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                                )
                            }
                        }

                        // Drawing baseline
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, size.height * 0.85f),
                            end = Offset(size.width, size.height * 0.85f),
                            strokeWidth = 2f
                        )
                    }

                    // Display code legends below graph
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        subjects.forEach { s ->
                            Text(s.code, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Subject cards list
        item {
            Text(
                "Course Logs & Stats",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CampusPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(subjects) { sub ->
            val rate = if (sub.totalClasses > 0) (sub.attendanceCount * 100) / sub.totalClasses else 0
            val isWarning = rate < 75

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${sub.code} • Faculty: ${sub.facultyName}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(
                            "$rate%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWarning) AlertError else CampusSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = if (sub.totalClasses > 0) sub.attendanceCount.toFloat() / sub.totalClasses else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isWarning) AlertError else CampusSecondary,
                        trackColor = Color(0xFFF1F5F9)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Attended: ${sub.attendanceCount}/${sub.totalClasses} classes",
                            fontSize = 11.sp,
                            color = if (isWarning) AlertError else Color.Gray,
                            fontWeight = if (isWarning) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isWarning) {
                            TextButton(
                                onClick = { showLeaveDialog = sub },
                                colors = ButtonDefaults.textButtonColors(contentColor = AlertError)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply Leave", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = { showLeaveDialog = sub }) {
                                Text("Apply Leave", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Section: Historic Logs
        item {
            Text(
                "Daily Check-In History",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CampusPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(logs) { log ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(log.subjectName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${log.subjectCode} • ${log.date}", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.remarks, fontSize = 11.sp, color = CampusPrimary, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                when (log.status) {
                                    "Present" -> AlertSuccess.copy(alpha = 0.15f)
                                    "Absent" -> AlertError.copy(alpha = 0.15f)
                                    else -> AlertWarning.copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = log.status,
                            color = when (log.status) {
                                "Present" -> AlertSuccess
                                "Absent" -> AlertError
                                else -> AlertWarning
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Leave Apply dialog sheet
    showLeaveDialog?.let { sub ->
        var leaveReason by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showLeaveDialog = null }) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Apply Medical/Duty Leave", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CampusPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Course: ${sub.name} (${sub.code})", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = leaveReason,
                        onValueChange = { leaveReason = it },
                        label = { Text("Reason for absence") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showLeaveDialog = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (leaveReason.trim().isNotEmpty()) {
                                viewModel.applyLeaveRequest(sub.code, leaveReason)
                                showLeaveDialog = null
                                Toast.makeText(context, "Leave applied to HOD for review!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please state a reason.", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: ACADEMICS (SUBJECTS & SYLLABUS) ---

@Composable
fun TabAcademics(viewModel: CampusViewModel) {
    val subjects by viewModel.academicSubjects.collectAsStateWithLifecycle()
    var selectedSubjectDetails by remember { mutableStateOf<SubjectEntity?>(null) }
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Courses, 1 = Grade History
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(selectedTabIndex = activeSubTab) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }, text = { Text("Courses & Faculty", fontSize = 13.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }, text = { Text("Transcript / Grades", fontSize = 13.sp, fontWeight = FontWeight.Bold) })
        }

        if (activeSubTab == 0) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        "Your Core Sem-5 Curriculum",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CampusPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(subjects) { sub ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedSubjectDetails = sub },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(CampusPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sub.code.take(2), fontWeight = FontWeight.Bold, color = CampusPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${sub.code} • ${sub.credits} Credits", fontSize = 11.sp, color = Color.Gray)
                                Text("Classroom: ${sub.classroom} • ${sub.facultyName}", fontSize = 11.sp, color = CampusSecondary)
                            }
                            Icon(Icons.Default.Info, contentDescription = "Syllabus Details", tint = CampusSecondary)
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CampusPrimary)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PROVISIONAL ACADEMIC RECORD", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cumulative GPA: 8.92", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Total Completed Credits: 76 / 150", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    Toast.makeText(context, "Generating secure transcript...", Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        delay(1500)
                                        Toast.makeText(context, "Transcript downloaded: aarav_sharma_transcript.pdf", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CampusPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Official Transcript", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Text(
                        "Grades Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CampusPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(subjects) { sub ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Midterm / Internal Assessment: ${sub.internalMarks}/50", fontSize = 11.sp, color = Color.Gray)
                            }
                            Box(
                                modifier = Modifier
                                    .background(CampusSecondary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(sub.grade, fontWeight = FontWeight.Bold, color = CampusSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Syllabus Details sheet
    selectedSubjectDetails?.let { sub ->
        Dialog(onDismissRequest = { selectedSubjectDetails = null }) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CampusPrimary)
                    Text("${sub.code} • ${sub.credits} Credits • Class ${sub.classroom}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Instructor Contact:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${sub.facultyName} (${sub.facultyEmail})", fontSize = 12.sp, color = CampusSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Syllabus Curriculum:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(sub.syllabus, fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { selectedSubjectDetails = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

// --- TAB 4: PAYMENTS (FEES PORTAL) ---

@Composable
fun TabPayments(viewModel: CampusViewModel) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val tuitionPaid by viewModel.tuitionFeePaid.collectAsStateWithLifecycle()
    val totalTuition = viewModel.totalTuitionFee
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activePaymentPurpose by remember { mutableStateOf("Tuition Semester 5") }
    var inputAmountToPay by remember { mutableStateOf("30000") }
    var showGatewayDialog by remember { mutableStateOf(false) }
    var showReceiptDetails by remember { mutableStateOf<PaymentReceipt?>(null) }
    var isPayingSimulation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Core Tuition Fees progress Card
        item {
            val due = totalTuition - tuitionPaid
            val rate = tuitionPaid.toFloat() / totalTuition.toFloat()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tuition Balance (Academic Year 2026)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CampusPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Tuition", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${"%,.0f".format(totalTuition)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CampusPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pending Balance", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                if (due > 0) "₹${"%,.0f".format(due)}" else "Fully Paid! 🎉",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (due > 0) AlertError else AlertSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = rate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (due > 0) CampusSecondary else AlertSuccess
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${(rate * 100).toInt()}% of fee paid (₹${"%,.0f".format(tuitionPaid)} / ₹${"%,.0f".format(totalTuition)})",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Payment gateway portal input card
        item {
            val due = totalTuition - tuitionPaid
            if (due > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Make Secure Online Payment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CampusPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = activePaymentPurpose,
                            onValueChange = { activePaymentPurpose = it },
                            label = { Text("Payment Purpose") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputAmountToPay,
                            onValueChange = { inputAmountToPay = it },
                            label = { Text("Amount (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val cleanAmt = inputAmountToPay.toDoubleOrNull()
                                if (cleanAmt != null && cleanAmt > 0 && cleanAmt <= due) {
                                    showGatewayDialog = true
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount (Max: ₹$due)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Proceed to Gateway (₹$inputAmountToPay)")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // History logs
        item {
            Text(
                "Transaction Receipt History",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CampusPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(receipts) { rec ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { showReceiptDetails = rec },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = CampusSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rec.purpose, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Date: ${rec.date} • ID: ${rec.id}", fontSize = 10.sp, color = Color.Gray)
                        Text("Paid via: ${rec.method}", fontSize = 10.sp, color = CampusPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${"%,.0f".format(rec.amount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CampusSecondary)
                        Box(
                            modifier = Modifier
                                .background(AlertSuccess.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(rec.status, color = AlertSuccess, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Razorpay/Stripe Simulator gate dialog
    if (showGatewayDialog) {
        var cardNo by remember { mutableStateOf("4111 2222 3333 4444") }
        var cardExpiry by remember { mutableStateOf("12/29") }
        var cardCvv by remember { mutableStateOf("123") }
        var currentMethod by remember { mutableStateOf("Razorpay") }

        Dialog(onDismissRequest = { if (!isPayingSimulation) showGatewayDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Campus Pay Gateway", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CampusPrimary)
                    Text("Amount to Pay: ₹$inputAmountToPay", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = currentMethod == "Razorpay",
                            onClick = { currentMethod = "Razorpay" },
                            label = { Text("Razorpay SDK", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = currentMethod == "Card",
                            onClick = { currentMethod = "Card" },
                            label = { Text("Card", fontSize = 11.sp) },
                            modifier = Modifier.weight(0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = currentMethod == "UPI",
                            onClick = { currentMethod = "UPI" },
                            label = { Text("UPI", fontSize = 11.sp) },
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentMethod == "Razorpay") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CampusPrimary.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CampusPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_campus_logo),
                                    contentDescription = "Razorpay Logo",
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SECURE RAZORPAY GATEWAY",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = CampusPrimary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Includes native payment flows, credit/debit card forms, netbanking, and UPI directly powered by Razorpay.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else if (currentMethod == "Card") {
                        OutlinedTextField(
                            value = cardNo,
                            onValueChange = { cardNo = it },
                            label = { Text("Card Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = cardExpiry,
                                onValueChange = { cardExpiry = it },
                                label = { Text("Expiry (MM/YY)") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = cardCvv,
                                onValueChange = { cardCvv = it },
                                label = { Text("CVV") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = "UPI: aarav.sharma@okicici\nWe will trigger a collect request to your banking app.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = CampusSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isPayingSimulation) {
                        CircularProgressIndicator(color = CampusSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Securing payment loop...", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showGatewayDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (currentMethod == "Razorpay") {
                                    val amt = inputAmountToPay.toDoubleOrNull() ?: 0.0
                                    val activity = context as? com.example.MainActivity
                                    if (activity != null) {
                                        activity.startRazorpayPayment(amt, activePaymentPurpose)
                                        showGatewayDialog = false
                                    } else {
                                        Toast.makeText(context, "Error: Payment Activity unavailable.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    isPayingSimulation = true
                                    scope.launch {
                                        delay(2000)
                                        viewModel.makePayment(
                                            amount = inputAmountToPay.toDoubleOrNull() ?: 0.0,
                                            purpose = activePaymentPurpose,
                                            method = currentMethod
                                        )
                                        isPayingSimulation = false
                                        showGatewayDialog = false
                                        Toast.makeText(context, "Payment Captured Successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Text(if (currentMethod == "Razorpay") "Launch Razorpay" else "Pay Securely")
                            }
                        }
                    }
                }
            }
        }
    }

    // Dynamic Visual Receipt stamp Dialogue
    showReceiptDetails?.let { rec ->
        Dialog(onDismissRequest = { showReceiptDetails = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("UNIVERSITY FEES RECEIPT", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CampusPrimary)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Receipt Number:", fontSize = 11.sp, color = Color.Gray)
                        Text(rec.id, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment Date:", fontSize = 11.sp, color = Color.Gray)
                        Text(rec.date, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid For:", fontSize = 11.sp, color = Color.Gray)
                        Text(rec.purpose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid Via:", fontSize = 11.sp, color = Color.Gray)
                        Text(rec.method, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Amount Paid", fontSize = 11.sp, color = Color.Gray)
                    Text("₹${"%,.0f".format(rec.amount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CampusSecondary)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated stamp design vector on Canvas!
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawCircle(color = AlertSuccess, radius = 35f, style = Stroke(width = 3f))
                        drawCircle(color = AlertSuccess.copy(alpha = 0.1f), radius = 32f)
                    }
                    Text("PAID ONLINE", fontWeight = FontWeight.Bold, color = AlertSuccess, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Saving PDF receipt to Storage...", Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    delay(1000)
                                    Toast.makeText(context, "Receipt saved: ${rec.id}.pdf", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Download PDF")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showReceiptDetails = null }, modifier = Modifier.weight(1f)) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 5: AI CHAT (GEMINI CHATBOT HELPER) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabAiChat(viewModel: CampusViewModel) {
    val history by viewModel.aiChatHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var inputMsg by remember { mutableStateOf("") }
    val localKeyboard = LocalSoftwareKeyboardController.current

    // Prompt suggest chips
    val suggestions = listOf(
        "Check OS Attendance",
        "Am I ready for exams?",
        "Show my total fees balance",
        "Suggest a study planner"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Chat History List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                history.forEach { msg ->
                    val isAssistant = msg is ChatMessage.Assistant
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isAssistant) Arrangement.Start else Arrangement.End
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isAssistant) 4.dp else 16.dp,
                                bottomEnd = if (isAssistant) 16.dp else 4.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAssistant) {
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                } else {
                                    CampusPrimary
                                }
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isAssistant) "CampusAI" else "You",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAssistant) CampusSecondary else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = when (msg) {
                                        is ChatMessage.User -> msg.text
                                        is ChatMessage.Assistant -> msg.text
                                    },
                                    fontSize = 13.sp,
                                    color = if (isAssistant) MaterialTheme.colorScheme.onSurface else Color.White
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CampusSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CampusAI is reading ERP records...", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Scroll down automatically when history updates
            LaunchedEffect(history.size, isLoading) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        // Suggestions Box
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(suggestions) { sug ->
                AssistChip(
                    onClick = {
                        viewModel.askGemini(sug)
                        localKeyboard?.hide()
                    },
                    label = { Text(sug, fontSize = 11.sp) }
                )
            }
        }

        // Input Controls Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputMsg,
                    onValueChange = { inputMsg = it },
                    placeholder = { Text("Ask anything about ERP...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CampusSecondary,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputMsg.trim().isNotEmpty()) {
                            viewModel.askGemini(inputMsg)
                            inputMsg = ""
                            localKeyboard?.hide()
                        }
                    },
                    modifier = Modifier
                        .background(CampusPrimary, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
