package com.arieldavid.gymapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arieldavid.gymapplication.local.QrLocal.LocalApi
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

import LoginScreen
import NuevaPasswordScreen
import RecuperarPasswordScreen
import com.arieldavid.gymapplication.local.QrLocal.NetworkScanner

class MainActivity : ComponentActivity() {

    private var servidorListo by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Si hay una IP guardada, se configura al instante (sin red) para que
// las pantallas puedan usar LocalApi desde el primer momento.
        getSharedPreferences("server", MODE_PRIVATE)
            .getString("server_ip", null)
            ?.let { aplicarIp(it) }

// Y se verifica / busca en segundo plano
        lifecycleScope.launch {

            val ip = NetworkScanner.buscarServidor(this@MainActivity)

            if (ip != null) {
                aplicarIp(ip)
                servidorListo = true
                Log.d("SERVER", "Servidor GYM: $ip")
            } else {
                servidorListo = false
                Log.d("SERVER", "Servidor GYM no encontrado")
            }
        }




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                100
            )

        }


        NotificationHelper.createChannel(this)

        WorkScheduler.schedule(this)


        enableEdgeToEdge()



        setContent {


            GymApplicationTheme {


                val navController = rememberNavController()


                val currentRoute =
                    navController
                        .currentBackStackEntryAsState()
                        .value
                        ?.destination
                        ?.route



                val prefs =
                    getSharedPreferences(
                        "auth",
                        MODE_PRIVATE
                    )


                val token =
                    prefs.getString(
                        "access_token",
                        null
                    )


                val clienteVinculado = prefs.getString(
                    "linked_client_id",
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

                        startDestination =
                        if (token != null || clienteVinculado != null) {
                            "home"
                        } else {
                            "login"
                        },

                        modifier =
                        Modifier.padding(padding)

                    ) {



                        composable("login") {


                            LoginScreen(

                                navController = navController,

                                servidorListo = servidorListo

                            )

                        }



                        composable("recuperar") {

                            RecuperarPasswordScreen(
                                navController
                            )

                        }



                        composable("nueva_password") {

                            NuevaPasswordScreen(
                                navController
                            )

                        }



                        composable("home") {

                            HomeScreen(
                                navController
                            )

                        }



                        composable("planes") {

                            PlanesScreen(
                                navController
                            )

                        }



                        composable("entrenadores") {

                            TrainerScreen(
                                navController
                            )

                        }



                        composable("yo") {

                            ProfileScreen(
                                navController
                            )

                        }


                    }

                }


            }


        }


    }
    private fun aplicarIp(ip: String) {
        SpringRepository.actualizarIp(ip)
        LocalApi.actualizarIp(ip)
    }

}