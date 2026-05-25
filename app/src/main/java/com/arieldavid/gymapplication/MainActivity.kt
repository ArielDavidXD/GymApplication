package com.arieldavid.gymapplication


import LoginScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arieldavid.gymapplication.ui.colores.MainBackground
import com.arieldavid.gymapplication.ui.home.BottomBar
import com.arieldavid.gymapplication.ui.home.HomeScreen
import com.arieldavid.gymapplication.ui.home.PlanesScreen
import com.arieldavid.gymapplication.ui.profile.ProfileScreen
import com.arieldavid.gymapplication.ui.theme.GymApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GymApplicationTheme {
                val navController = rememberNavController()
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                Scaffold(
                    containerColor = MainBackground,
                    bottomBar = {
                        if (currentRoute != "login") {
                            BottomBar(navController)
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("login") {
                            LoginScreen(navController)
                        }
                        composable("home") {
                            HomeScreen(navController)
                        }
                        composable("planes") {
                            PlanesScreen(navController)
                        }
                        composable("entrenadores") {
                            Text("Pantalla Entrenadores")
                        }
                        composable("yo") {
                            ProfileScreen(navController)
                        }
                    }
                }
            }
        }
    }
}
