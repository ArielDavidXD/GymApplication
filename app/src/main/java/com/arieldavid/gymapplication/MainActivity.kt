package com.arieldavid.gymapplication

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.arieldavid.gymapplication.workers.NotificacionWorker
import java.util.concurrent.TimeUnit
import android.Manifest
import LoginScreen
import NuevaPasswordScreen
import RecuperarPasswordScreen
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arieldavid.gymapplication.local.QrLocal.LocalApi
import com.arieldavid.gymapplication.local.QrLocal.NetworkScanner
import com.arieldavid.gymapplication.local.QrLocal.SpringRepository
import com.arieldavid.gymapplication.ui.colores.MainBackground
import com.arieldavid.gymapplication.ui.home.BottomBar
import com.arieldavid.gymapplication.ui.home.HomeScreen
import com.arieldavid.gymapplication.ui.home.PlanesScreen
import com.arieldavid.gymapplication.ui.notifications.NotificationHelper
import com.arieldavid.gymapplication.ui.notifications.WorkScheduler
import com.arieldavid.gymapplication.ui.profile.ProfileScreen
import com.arieldavid.gymapplication.ui.theme.GymApplicationTheme
import com.arieldavid.gymapplication.ui.trainers.TrainerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            SpringRepository.actualizarIp("192.168.1.106")
            LocalApi.actualizarIp("192.168.1.106")

            Log.d("SERVER", "IP fija configurada")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        NotificationHelper.createChannel(this)

// Programar el Worker (una sola vez)
        WorkScheduler.schedule(this)

        enableEdgeToEdge()

        setContent {

            GymApplicationTheme {

                val navController = rememberNavController()
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route

                val prefs = getSharedPreferences(
                    "auth",
                    MODE_PRIVATE
                )

                val token = prefs.getString(
                    "access_token",
                    null
                )

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
                        startDestination = if (token != null) "home" else "login",
                        modifier = Modifier.padding(padding)
                    ) {

                        composable("login") {
                            LoginScreen(navController)
                        }

                        composable("recuperar") {
                            RecuperarPasswordScreen(navController)
                        }

                        composable("nueva_password") {
                            NuevaPasswordScreen(navController)
                        }

                        composable("home") {
                            HomeScreen(navController)
                        }

                        composable("planes") {
                            PlanesScreen(navController)
                        }

                        composable("entrenadores") {
                            TrainerScreen(navController)
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

