package com.theylor.prisma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.theylor.prisma.ui.theme.*

@Composable
fun Register(onRegisterSuccess: () -> Unit = {}, onBackClick: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val auth = remember { FirebaseAuth.getInstance() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrismaDarkBackground, Color.Black)
                )
            )
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CREAR CUENTA",
                color = PrismaRed,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrismaCardBackground),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                ) {
                    // Campo Nombre
                    Text("Nombre Completo", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Ingrese su nombre", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrismaRed,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Email
                    Text("Email", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Ingrese su correo electrónico", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrismaRed,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Password
                    Text("Contraseña", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Mínimo 6 caracteres", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrismaRed,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    errorMessage?.let {
                        Text(it, color = PrismaRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            val cleanEmail = email.trim()
                            val cleanPassword = password.trim()

                            if (cleanName.isEmpty() || cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
                                errorMessage = "Por favor, completa todos los campos"
                            } else if (cleanPassword.length < 6) {
                                errorMessage = "La contraseña debe tener al menos 6 caracteres"
                            } else {
                                isLoading = true
                                errorMessage = null
                                auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            val profileUpdates = userProfileChangeRequest {
                                                displayName = cleanName
                                            }
                                            user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                                isLoading = false
                                                onRegisterSuccess()
                                            } ?: run {
                                                isLoading = false
                                                onRegisterSuccess()
                                            }
                                        } else {
                                            isLoading = false
                                            val error = task.exception?.localizedMessage ?: ""
                                            errorMessage = when {
                                                error.contains("already in use", ignoreCase = true) || error.contains("email-already-in-use", ignoreCase = true) ->
                                                    "El correo ya está registrado"
                                                error.contains("badly formatted", ignoreCase = true) || error.contains("invalid-email", ignoreCase = true) ->
                                                    "El correo electrónico no es válido"
                                                error.contains("at least 6 characters", ignoreCase = true) || error.contains("weak-password", ignoreCase = true) ->
                                                    "La contraseña debe tener al menos 6 caracteres"
                                                else -> "Error al registrar la cuenta"
                                            }
                                        }
                                    }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismaRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("REGISTRARME", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
