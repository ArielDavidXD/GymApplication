package com.arieldavid.gymapplication.ui.home

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.arieldavid.gymapplication.ui.colores.Cards
import com.arieldavid.gymapplication.ui.colores.MainBackground
import com.arieldavid.gymapplication.ui.colores.TextoPrincipal
import com.arieldavid.gymapplication.ui.colores.TextoSecundario
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.arieldavid.gymapplication.ui.MembresiaRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.MembresiaEntity
import com.arieldavid.gymapplication.ui.plans.PlansRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import com.arieldavid.gymapplication.AuthClass

@Serializable
data class Membresia(
    val id_membresia: Int? = null,
    val nombre: String? = null,
    val precio_normal: Double? = null,
    val precio_transferencia: Double? = null,
    val beneficio: String? =null
)


@Composable
fun PlanesScreen(navController: NavController) {

    val context = LocalContext.current

    var idMembresiaActual by remember {
        mutableStateOf<Int?>(null)
    }

    var membresias by remember {
        mutableStateOf<List<Membresia>>(emptyList())
    }

    val repository = remember {
        PlansRepository()
    }

    LaunchedEffect(Unit) {

        val db = DatabaseProvider.getDatabase(context)

        val cliente = withContext(Dispatchers.IO) {
            db.clientDAO().getProfile()
        }

        idMembresiaActual = cliente?.idMembresia

        // Primero cargar desde Room
        membresias = repository.obtenerMembresiaLocal(context)

        Log.d("PLANES", "ROOM -> ${membresias.size}")

        val token = AuthClass.getAccessToken(context)

        if (token == null) {

            // Si ya existen datos locales, seguimos trabajando offline
            if (membresias.isNotEmpty()) {
                Log.d("PLANES", "Modo offline")
                return@LaunchedEffect
            }

            navController.navigate("login") {
                popUpTo(0)
            }

            return@LaunchedEffect
        }

        try {

            // actualizarOnline() ya guarda en Room
            membresias = repository.actualizarOnline(
                context,
                token
            )

            Log.d("PLANES", "ONLINE -> ${membresias.size}")

        } catch (e: Exception) {

            Log.e("PLANES", "Error sincronizando", e)

        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .statusBarsPadding()
            .padding(16.dp)
    ) {

        Text(
            text = "Planes de Membresía",
            color = TextoPrincipal,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Elige el plan que mejor se adapte a tus objetivos.",
            color = TextoSecundario,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        val basic = membresias.find { it.id_membresia == 1 }
        val premium = membresias.find { it.id_membresia == 2 }

        basic?.let {

            PlanCardBasic(
                membresia = it,
                esActual = it.id_membresia == idMembresiaActual
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        premium?.let {

            PlanCardPremium(
                membresia = it,
                esActual = it.id_membresia == idMembresiaActual
            )
        }
    }
}


@Composable
fun PlanCardBasic(
    membresia: Membresia,
    esActual: Boolean
) {

    Card(
        colors = CardDefaults.cardColors(containerColor = Cards),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = TextoPrincipal
                    )

                    Text(
                        text = "BÁSICO",
                        color = TextoPrincipal,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (esActual) {
                    Text(
                        text = "Tu membresía",
                        color = TextoPrincipal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Precio normal: $${membresia.precio_normal ?: 0.0}",
                color = TextoPrincipal,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Transferencia: $${membresia.precio_transferencia ?: 0.0}",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                "/Mes",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "• ${membresia.beneficio}",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
@Composable
fun PlanCardPremium(
    membresia: Membresia,
    esActual: Boolean
) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF303030)
        ),

        shape = RoundedCornerShape(20.dp),

        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFFD700)
                    )

                    Text(
                        text = "PREMIUM",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (esActual) {
                    Text(
                        text = "Tu membresía",
                        color = Color(0xFFE0E0E0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Precio normal: $${membresia.precio_normal ?: 0.0}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Transferencia: $${membresia.precio_transferencia ?: 0.0}",
                color = Color(0xFFBDBDBD),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                "/Mes",
                color = Color(0xFFAAAAAA)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "• ${membresia.beneficio}",
                color = Color(0xFFAAAAAA)
            )

            Spacer(modifier = Modifier.height(16.dp))

        }
    }
}