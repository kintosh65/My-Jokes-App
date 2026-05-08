package com.kintosh.myjokesapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.*
import com.kintosh.myjokesapp.UserManager

@Composable
fun FunnyLoginScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 🎬 Background Animation
    val backgroundAnim by rememberLottieComposition(LottieCompositionSpec.Asset("funny_background.json"))
    val backgroundProgress by animateLottieCompositionAsState(
        composition = backgroundAnim,
        iterations = LottieConstants.IterateForever
    )

    fun handleAuth() {
        errorMessage = null
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Don't ghost us — fill in both fields 😅"
            return
        }

        if (isSignUpMode) {
            if (UserManager.signUp(context, username, password)) {
                Toast.makeText(context, "Account created! Now log in 🎭", Toast.LENGTH_SHORT).show()
                isSignUpMode = false
            } else {
                errorMessage = "Username already taken! Try something funnier 🤡"
            }
        } else {
            if (UserManager.login(context, username, password)) {
                Toast.makeText(context, "Welcome back, $username! 😂", Toast.LENGTH_SHORT).show()
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                errorMessage = "Oops! Wrong punchline (credentials) 😬"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🔄 Lottie Background
        LottieAnimation(
            composition = backgroundAnim,
            isPlaying = true,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.fillMaxSize()
        )

        // 💻 Auth Form with Glassmorphism
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUpMode) "Join Kadafa Jokes! ✨" else "Welcome to Kadafa Jokes!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Text(
                        text = if (isSignUpMode) "Create your comedy profile 📝" else "Sign in to unlock the funny 🔓🤣",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Comedy Stage Name 🎤") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Secret Joke Code 🔐") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { handleAuth() },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (isSignUpMode) "Sign Me Up! 🚀" else "Let Me In 😂",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { 
                            isSignUpMode = !isSignUpMode
                            errorMessage = null
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            if (isSignUpMode) "Already a regular? Log in here" else "New here? Create a comedy profile!",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
