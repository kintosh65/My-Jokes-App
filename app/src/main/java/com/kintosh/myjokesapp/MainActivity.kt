package com.kintosh.myjokesapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kintosh.myjokesapp.ui.theme.MyjokesappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyjokesappTheme  {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                )
                 {
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            FunnyLoginScreen(navController)
                        }
                        composable("home") {
                            HomeScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FunnyLoginScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 🎬 Background Animation
    val backgroundAnim by rememberLottieComposition(LottieCompositionSpec.Asset("funny_background.json"))
    val backgroundProgress by animateLottieCompositionAsState(
        composition = backgroundAnim,
        iterations = LottieConstants.IterateForever
    )

    fun handleLogin() {
        errorMessage = null
        when {
            username.isBlank() || password.isBlank() -> {
                errorMessage = "Don't ghost us — fill in both fields 😅"
            }
            username == "admin" && password == "1234" -> {
                Toast.makeText(context, "You're in! Let the laughter begin 😂", Toast.LENGTH_SHORT).show()
                username = ""
                password = ""
                navController.navigate("home") {
                    popUpTo("login") {
                        this.inclusive = true
                    }
                }
            }
            else -> {
                errorMessage = "Oops! Wrong punchline 😬"
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


        // 💻 Login Form Layered On Top
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to LOLand!",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Sign in to unlock the funny 🔓🤣",
                fontSize = 16.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Comedy Stage Name 🎤") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Secret Joke Code 🔐") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { handleLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Let Me In 😂", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Optional: Static joke at the bottom
            Text(
                text = "💡 Joke of the day: Why don’t skeletons fight each other? They don’t have the guts!",
                fontSize = 14.sp,
                color = Color(0xFF616161),
                textAlign = TextAlign.Center
            )
        }
    }
}
