package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.SupabaseAuth
import com.example.data.remote.SupabaseConfig
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    supabaseAuth: SupabaseAuth,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isConfigured = SupabaseConfig.isConfigured

    // Shake animation for error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            shakeOffset.animateTo(12f, tween(40))
            shakeOffset.animateTo(-12f, tween(40))
            shakeOffset.animateTo(8f, tween(40))
            shakeOffset.animateTo(-8f, tween(40))
            shakeOffset.animateTo(0f, tween(40))
        }
    }

    val submitInteractionSource = remember { MutableInteractionSource() }
    val isSubmitPressed by submitInteractionSource.collectIsPressedAsState()
    val submitScale by animateFloatAsState(
        if (isSubmitPressed) 0.95f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "loginButtonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // A. Curved Dark Blue Header with Logo in White Circle & Husn e Zar Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBlueDeep, DarkBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                // White circle containing logo
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Husn e Zar Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Husn e Zar",
                    color = Yellow,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Admin Portal & Cloud Sync",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // B. White Login Card for Input Fields
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("login_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign In",
                        color = DarkBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isConfigured) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = AlertYellowBg),
                            border = BorderStroke(1.dp, Yellow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Config Warning",
                                    tint = YellowDeep,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Supabase URL aur Anon Key configure nahi hain. AI Studio Secrets panel mein SUPABASE_URL aur SUPABASE_ANON_KEY add karein.",
                                    color = DarkBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Inline Error with Shake Animation
                    if (errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationX = shakeOffset.value }
                                .background(AlertRedBg, RoundedCornerShape(12.dp))
                                .border(1.dp, AlertRed, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = AlertRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Admin Email", color = DarkBlue, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("admin@husnezar.com", color = DarkBlue.copy(alpha = 0.5f), fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = DarkBlue
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = standardTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password", color = DarkBlue, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("••••••••", color = DarkBlue.copy(alpha = 0.5f), fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Icon",
                                tint = DarkBlue
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                    tint = DarkBlue
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
                                    performLogin(
                                        email = email,
                                        password = password,
                                        supabaseAuth = supabaseAuth,
                                        coroutineScope = coroutineScope,
                                        onLoadingChange = { isLoading = it },
                                        onError = { errorMessage = it },
                                        onSuccess = onLoginSuccess
                                    )
                                }
                            }
                        ),
                        colors = standardTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button with Press Scale Animation and Loading Spinner
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Email aur password dono likhein."
                                return@Button
                            }
                            performLogin(
                                email = email,
                                password = password,
                                supabaseAuth = supabaseAuth,
                                coroutineScope = coroutineScope,
                                onLoadingChange = { isLoading = it },
                                onError = { errorMessage = it },
                                onSuccess = onLoginSuccess
                            )
                        },
                        enabled = !isLoading,
                        interactionSource = submitInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .graphicsLayer { scaleX = submitScale; scaleY = submitScale }
                            .testTag("login_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Yellow,
                            contentColor = DarkBlue,
                            disabledContainerColor = Yellow.copy(alpha = 0.4f),
                            disabledContentColor = DarkBlue.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = DarkBlue,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Login ho raha hai...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBlue
                            )
                        } else {
                            Text(
                                text = "Login Karein",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBlue
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

private fun performLogin(
    email: String,
    password: String,
    supabaseAuth: SupabaseAuth,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    onLoadingChange(true)
    onError("")
    coroutineScope.launch {
        val result = supabaseAuth.login(email = email, password = password)
        onLoadingChange(false)
        result.fold(
            onSuccess = {
                onSuccess()
            },
            onFailure = { error ->
                val errorMsg = when {
                    error.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                        "Email ya password ghalat hai."
                    error.message?.contains("configure nahi hain", ignoreCase = true) == true ->
                        error.message ?: "Supabase configuration missing hai."
                    error.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    error.message?.contains("Failed to connect", ignoreCase = true) == true ->
                        "Internet connection check karein."
                    else ->
                        "Login fail: ${error.localizedMessage ?: "Ghalat credentials"}"
                }
                onError(errorMsg)
            }
        )
    }
}
