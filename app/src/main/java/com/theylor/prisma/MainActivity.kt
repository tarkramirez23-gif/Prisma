package com.theylor.prisma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.FirebaseApp
import com.theylor.prisma.ui.screens.Home
import com.theylor.prisma.ui.screens.Login
import com.theylor.prisma.ui.screens.Peliculas
import com.theylor.prisma.ui.screens.Perfil
import com.theylor.prisma.ui.screens.ProfileSettings
import com.theylor.prisma.ui.screens.Register
import com.theylor.prisma.ui.screens.Resenas
import com.theylor.prisma.ui.theme.PRISMATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicialización manual de Firebase por si el plugin no carga
        FirebaseApp.initializeApp(this)
        
        enableEdgeToEdge()
        setContent {
            PRISMATheme {
                var currentScreen by remember { mutableStateOf("home") }
                var tipoPerfil by remember { mutableStateOf("Espectador") }
                
                when (currentScreen) {
                    "home" -> Home(onComenzarClick = { currentScreen = "login" })
                    "login" -> Login(
                        onLoginSuccess = { currentScreen = "perfil" },
                        onCreateAccountClick = { currentScreen = "register" }
                    )
                    "register" -> Register(
                        onRegisterSuccess = { currentScreen = "perfil" },
                        onBackClick = { currentScreen = "login" }
                    )
                    "perfil" -> Perfil(onSelectPerfil = { 
                        if (it == "resenas") {
                            tipoPerfil = "Espectador"
                            currentScreen = "resenas"
                        } else {
                            tipoPerfil = "Cinefilo"
                            currentScreen = "peliculas"
                        }
                    })
                    "resenas" -> Resenas(
                        onHomeTabClick = { currentScreen = "peliculas" },
                        onProfileTabClick = { currentScreen = "profile_settings" }
                    )
                    "peliculas" -> Peliculas(
                        onProfileTabClick = { currentScreen = "profile_settings" }
                    )
                    "profile_settings" -> ProfileSettings(
                        currentTipo = tipoPerfil,
                        onCambiarPerfilClick = {
                            if (tipoPerfil == "Espectador") {
                                tipoPerfil = "Cinefilo"
                                currentScreen = "peliculas"
                            } else {
                                tipoPerfil = "Espectador"
                                currentScreen = "resenas"
                            }
                        },
                        onSignOutClick = {
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
}
