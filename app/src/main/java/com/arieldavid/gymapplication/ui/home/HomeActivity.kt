
package com.arieldavid.gymapplication.ui.home
import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arieldavid.gymapplication.R
import androidx.compose.ui.platform.LocalContext
import com.arieldavid.gymapplication.ui.colores.*
import com.arieldavid.gymapplication.ui.theme.GymApplicationTheme
import androidx.compose.runtime.*
import com.arieldavid.gymapplication.SupabaseConfig
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.ClientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray


// 🔥 HOME
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var fechaPago by remember {
        mutableStateOf("-")
    }

    var loadingPago by remember {
        mutableStateOf(true)
    }
    var cargado by remember {
        mutableStateOf(false)
    }


    LaunchedEffect(Unit) {

            if (cargado) return@LaunchedEffect

            cargado = true



        val prefs = context.getSharedPreferences(
            "auth",
            Context.MODE_PRIVATE
        )

        val token = prefs.getString("access_token", null)

        val db = DatabaseProvider.getDatabase(context)
        val dao = db.clientDAO()

        if (token == null) {

            navController.navigate("login") {
                popUpTo(0)
            }

            return@LaunchedEffect
        }

        try {

            val client = OkHttpClient()
            Log.d("PERFIL", "ENTRO AL TRY")

            val userId = prefs.getString("user_id", null)

            val request = Request.Builder()
                .url(
                    "${SupabaseConfig.URL}/rest/v1/Clientes?user_id=eq.$userId&select=Payment(fecha_sig_pago)"
                )
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            Log.d("PERFIL", "REQUEST CREADO")

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            Log.d("PERFIL", "HTTP CODE: ${response.code}")


            val body = response.body?.string()

            println(body)

            if (!body.isNullOrEmpty()) {

                val jsonArray = JSONArray(body)

                if (jsonArray.length() > 0) {

                    val cliente = jsonArray.getJSONObject(0)

                    val payments = cliente.getJSONArray("Payment")

                    if (payments.length() > 0) {

                        fechaPago = payments
                            .getJSONObject(0)
                            .optString("fecha_sig_pago", "-")
                    }

                           withContext(Dispatchers.IO) {

                        val actual = dao.getProfile()

                        if (actual != null) {
                            dao.insert(
                                ClientEntity(
                                    uuid = actual.uuid,
                                    nombre = actual.nombre,
                                    telefono = actual.telefono,
                                    email = actual.email,
                                    idMembresia = actual.idMembresia,
                                    numeroCliente = actual.numeroCliente,
                                    fechaSigPago = fechaPago
                                )
                            )
                        }
                    }
                }
            }
            Log.d("PERFIL", "BODY: $body")


        } catch (e: Exception) {
            val local = withContext(Dispatchers.IO) {
                dao.getProfile()
            }

            if (local != null) {
                fechaPago = local.fechaSigPago ?: "-"
            }


            Log.e(
                "PERFIL",
                "ERROR: ${e.message}"
            )
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .statusBarsPadding()
            .padding(top = 10.dp)
            .padding(horizontal = 16.dp)
    ) {

        TopBar() // 🔥 AGREGA ESTO (no lo estabas llamando)

        Spacer(modifier = Modifier.height(20.dp))

        Greeting()

        Spacer(modifier = Modifier.height(20.dp))

        StatsRow(fechaPago)

        Spacer(modifier = Modifier.height(30.dp))

        QRSection()

        Spacer(modifier = Modifier.height(30.dp))
    }
}

//////////////////////////////////////////////////////////
// 🔝 TOP BAR (SIN ICONS)
//////////////////////////////////////////////////////////
@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MainBackground)
            .statusBarsPadding() // 🔥 ESTO ES LA CLAVE
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(id = R.drawable.icono),
            contentDescription = "Logo",
            modifier = Modifier.size(75.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            "ALTAPINTA GYM",
            color = TextoPrincipal,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontSize = 26.sp
        )
    }
}

//////////////////////////////////////////////////////////
// 👋 SALUDO
//////////////////////////////////////////////////////////
@Composable
fun Greeting() {

    val context = LocalContext.current

    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    val nombre = prefs.getString("nombre", "Usuario")

    Column {

        Text(
            "Hola $nombre 👋",
            color = TextoPrincipal,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            "¿Listo para superar tus límites hoy?",
            color = TextoSecundario
        )
    }
}

//////////////////////////////////////////////////////////
// 📊 STATS
//////////////////////////////////////////////////////////
@Composable
fun StatsRow(fechaPago: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

        StatCardModern(
            titulo = "RACHA",
            valor = "12",
            subtitulo = "DÍAS SEGUIDOS",
            emoji = "🔥",
            modifier = Modifier.weight(1f)
        )

        StatCardModern(
            titulo = "PAGO",
            valor = fechaPago,
            subtitulo = "PROXIMO PAGO",
            emoji = "💳",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCardModern(
    titulo: String,
    valor: String,
    subtitulo: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Cards),
        shape = RoundedCornerShape(20.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji)
                Spacer(modifier = Modifier.width(8.dp))
                Text(titulo, color = TextoSecundario)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                valor,
                color = TextoPrincipal,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(subtitulo, color = TextoSecundario)
        }
    }
}

//////////////////////////////////////////////////////////
// 🎯 QR
//////////////////////////////////////////////////////////
@Composable
fun QRSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("ESCANEAR QR", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "PULSA PARA REGISTRAR TU ENTRADA",
            color = TextoSecundario,
            style = MaterialTheme.typography.labelSmall
        )
    }
}


//////////////////////////////////////////////////////////
// 🔽 BOTTOM BAR (SIN ICONS)
//////////////////////////////////////////////////////////
@Composable
fun BottomBar(navController: NavController) {

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(containerColor = MainBackground) {

        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = {
                navController.navigate("home") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            },
            icon = { Text("🏠") },
            label = { Text("HOME") }
        )

        NavigationBarItem(
            selected = currentRoute == "planes",
            onClick = {
                navController.navigate("planes") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            },
            icon = { Text("🏋️") },
            label = { Text("PLANS") }
        )

        NavigationBarItem(
            selected = currentRoute == "entrenadores",
            onClick = {
                navController.navigate("entrenadores") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            },
            icon = { Text("🎯") },
            label = { Text("TRAINERS") }
        )

        NavigationBarItem(
            selected = currentRoute == "yo",
            onClick = {
                navController.navigate("yo") {
                    popUpTo("home")
                    launchSingleTop = true
                }
            },
            icon = { Text("👤") },
            label = { Text("PROFILE") }
        )
    }
}
//////////////////////////////////////////////////////////
// 👀 PREVIEW
//////////////////////////////////////////////////////////
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun mostrar() {
    GymApplicationTheme {
        HomeScreen(navController = rememberNavController())
    }
}