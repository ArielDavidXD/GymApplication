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

@Serializable
data class Membresia(
    val id_membresia: Int? = null,
    val nombre: String? = null,
    val precio: Double? = null,
    val beneficio: String? =null
)


@Composable
fun PlanesScreen(navController: NavController) {

    val context = LocalContext.current

    var membresias by remember {
        mutableStateOf<List<Membresia>>(emptyList())
    }

    LaunchedEffect(Unit) {

        val prefs = context.getSharedPreferences(
            "auth",
            Context.MODE_PRIVATE
        )

        val token = prefs.getString(
            "access_token",
            null
        )
        val db = DatabaseProvider.getDatabase(context)
        val dao = db.MembresiaDAO()

        if (token == null) {

            navController.navigate("login") {
                popUpTo(0)
            }

            return@LaunchedEffect
        }

        try {

            Log.d("PLANES", "ENTRO AL TRY")

            val json = withContext(Dispatchers.IO) {
                MembresiaRepository.cargarMembresias()
            }

            Log.d("PLANES", "JSON: $json")

            if (!json.isNullOrEmpty()) {

                val lista = Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<List<Membresia>>(json)

                Log.d("PLANES", "LISTA PARSEADA: $lista")

                membresias = lista

                Log.d("PLANES", "STATE ACTUALIZADO")

                withContext(Dispatchers.IO) {

                    dao.insertAll(
                        lista.map {

                            Log.d("PLANES", "INSERTANDO: ${it.nombre}")

                            MembresiaEntity(
                                id_membresia = it.id_membresia ?: 0,
                                nombre = it.nombre,
                                precio = it.precio,
                                beneficio = it.beneficio
                            )
                        }
                    )
                }
            }

        } catch (e: Exception) {

            Log.e("PLANES", "ERROR GENERAL", e)

            val local = withContext(Dispatchers.IO) {
                dao.getAll()
            }

            Log.d("PLANES", "LOCAL: $local")

            membresias = local.map {

                Membresia(
                    id_membresia = it.id_membresia,
                    nombre = it.nombre,
                    precio = it.precio,
                    beneficio = it.beneficio
                )
            }
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
            text = "MEMBERSHIP PLANS",
            color = TextoPrincipal,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose your discipline.",
            color = TextoSecundario
        )

        Spacer(modifier = Modifier.height(20.dp))

        val basic = membresias.find {
            it.id_membresia ==1
        }

        val premium = membresias.find {
            it.id_membresia == 2
        }

        basic?.let {

            PlanCardBasic(it)

            Spacer(modifier = Modifier.height(16.dp))
        }

        premium?.let {

            PlanCardPremium(it)
        }
    }
}


@Composable
fun PlanCardBasic(
    membresia: Membresia
) {

    Card(
        colors = CardDefaults.cardColors(containerColor = Cards),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                "⚡ BASIC",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "$${membresia.precio}",
                color = TextoPrincipal,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "/MONTH",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "• ${membresia.beneficio}",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                )
            ) {

                Text(
                    "SELECT PLAN",
                    color = Color.Black
                )
            }
        }
    }
}
@Composable
fun PlanCardPremium(
    membresia: Membresia
) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray
        ),

        shape = RoundedCornerShape(20.dp),

        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    "⚡ PREMIUM",
                    color = Color.Black
                )

                Text(
                    "MOST POPULAR",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "$${membresia.precio}",
                color = Color.Black,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "/MONTH",
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "• ${membresia.beneficio}",
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {

                Text(
                    "UPGRADE NOW",
                    color = Color.White
                )
            }
        }
    }
}