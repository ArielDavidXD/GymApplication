
package com.arieldavid.gymapplication.ui.home
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.arieldavid.gymapplication.ui.profile.MembershipCard
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Payment
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
import com.arieldavid.gymapplication.AuthClass
import com.arieldavid.gymapplication.SupabaseConfig
import com.arieldavid.gymapplication.local.QrLocal.LocalQrRepository
import com.arieldavid.gymapplication.local.QrLocal.SpringRepository
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.ClientEntity
import com.arieldavid.gymapplication.repository.TrainerRepository
import com.arieldavid.gymapplication.ui.EntradasRepository
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale


// 🔥 HOME
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current




    var fechaPago by remember {
        mutableStateOf("-")
    }


    val repository = remember {
        HomeRepository()
    }

    LaunchedEffect(Unit) {

        //////////////////////////////////////////////////////
        // 1. CARGAR ROOM
        //////////////////////////////////////////////////////

        fechaPago = repository.obtenerFechaPagoLocal(context)

        val prefs = context.getSharedPreferences(
            "auth",
            Context.MODE_PRIVATE
        )

        //////////////////////////////////////////////////////
        // 2. TOKEN
        //////////////////////////////////////////////////////

        val token = AuthClass.getAccessToken(context)

        val userId = prefs.getString(
            "user_id",
            null
        )

        //////////////////////////////////////////////////////
        // 3. MODO OFFLINE
        //////////////////////////////////////////////////////

        if (token == null || userId == null) {

            if (fechaPago != "-") {

                Log.d("HOME", "Modo offline")

                return@LaunchedEffect
            }

            navController.navigate("login") {
                popUpTo(0)
            }

            return@LaunchedEffect
        }

        //////////////////////////////////////////////////////
        // 4. SINCRONIZAR ONLINE
        //////////////////////////////////////////////////////

        try {

            fechaPago = repository.obtenerFechaPago(
                context,
                token,
                userId
            )

        } catch (e: Exception) {

            Log.e("HOME", "Error sincronizando", e)

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


        Spacer(
            modifier = Modifier.height(16.dp)
        )

        MembershipCard(
            fechaPago = fechaPago
        )

        Spacer(modifier = Modifier.height(24.dp))

        QRSection(fechaPago = fechaPago)

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
            text = "¡Hola, $nombre!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Bienvenido nuevamente al gimnasio.",
            color = TextoSecundario,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}




//////////////////////////////////////////////////////////
// 🎯 QR
//////////////////////////////////////////////////////////
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun QRSection(
    fechaPago: String
) {
    var mostrarDialogo by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->

        Log.d("QR_DEBUG", "Callback ejecutado")

        if (result.contents != null) {

            Log.d("QR_DEBUG", "QR LEIDO: ${result.contents}")

            if (result.contents == "ALTAPINTA") {

                Log.d("QR_DEBUG", "QR CORRECTO")

                scope.launch {

                    val prefs = context.getSharedPreferences(
                        "auth",
                        Context.MODE_PRIVATE
                    )

                    val token = prefs.getString("access_token", "") ?: ""
                    val idCliente = prefs.getString("id_clientes", null)

                    if (idCliente == null) {

                        Toast.makeText(
                            context,
                            "Usuario no identificado",
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }

                    //=================== INTENTAR SPRING ===================

                    try {

                        val respuesta = withContext(Dispatchers.IO) {

                            LocalQrRepository.procesarQR(
                                qr = result.contents,
                                idCliente = idCliente
                            )
                        }

                        Log.d("QR", "OK: ${respuesta.ok}")
                        Log.d("QR", "MENSAJE: ${respuesta.mensaje}")

                        if (respuesta.ok) {

                            Toast.makeText(
                                context,
                                "Bienvenido ${respuesta.nombre}",
                                Toast.LENGTH_LONG
                            ).show()

                        } else {

                            Toast.makeText(
                                context,
                                respuesta.mensaje,
                                Toast.LENGTH_LONG
                            ).show()

                            if (respuesta.mensaje == "Solicitud enviada") {

                                repeat(120) { // Espera como máximo 1 minuto (120 x 500 ms)

                                    delay(500)

                                    val estado = try {

                                        withContext(Dispatchers.IO) {
                                            SpringRepository.obtenerEstadoSolicitud(idCliente)
                                        }

                                    } catch (e: Exception) {

                                        Toast.makeText(
                                            context,
                                            "Error consultando la solicitud",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        return@launch
                                    }

                                    Log.d("SOLICITUD", "Estado: $estado")

                                    when (estado?.trim()) {

                                        "PENDIENTE",
                                        "EN_REVISION" -> {
                                            // Sigue esperando
                                        }

                                        "ACEPTADA" -> {

                                            Toast.makeText(
                                                context,
                                                "✅ Entrada aprobada. ¡Bienvenido!",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            return@launch
                                        }

                                        "RECHAZADA" -> {

                                            Toast.makeText(
                                                context,
                                                "❌ Solicitud rechazada",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            return@launch
                                        }

                                        else -> {

                                            Toast.makeText(
                                                context,
                                                "Estado desconocido: $estado",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            return@launch
                                        }
                                    }
                                }

                                Toast.makeText(
                                    context,
                                    "La solicitud expiró. Inténtalo nuevamente.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        // Si Spring respondió correctamente,
                        // no seguimos con Supabase.
                        return@launch

                    } catch (e: Exception) {

                        Log.e("QR_LOCAL", "Servidor local no disponible", e)
                    }

                    //=================== RESPALDO SUPABASE ===================

                    try {

                        if (token.isBlank()) {

                            Toast.makeText(
                                context,
                                "Sesión inválida",
                                Toast.LENGTH_LONG
                            ).show()

                            return@launch
                        }

                        val yaEntro = withContext(Dispatchers.IO) {
                            EntradasRepository.yaEntroHoy(token, idCliente)
                        }

                        if (yaEntro) {

                            Toast.makeText(
                                context,
                                "Ya registraste tu entrada hoy",
                                Toast.LENGTH_LONG
                            ).show()

                            return@launch
                        }

                        val estaEnTurno = try {

                            withContext(Dispatchers.IO) {
                                EntradasRepository.verificarTurnoCliente(token, idCliente)
                            }

                        } catch (e: Exception) {

                            if (e.message == "GIMNASIO_CERRADO") {

                                Toast.makeText(
                                    context,
                                    "El gimnasio está cerrado",
                                    Toast.LENGTH_LONG
                                ).show()

                                return@launch
                            }

                            throw e
                        }

                        if (!estaEnTurno) {

                            val existe = withContext(Dispatchers.IO) {
                                EntradasRepository.existeSolicitudPendiente(
                                    token,
                                    idCliente
                                )
                            }

                            if (!existe) {

                                withContext(Dispatchers.IO) {
                                    EntradasRepository.crearSolicitudEntrada(
                                        token,
                                        idCliente
                                    )
                                }

                                Toast.makeText(
                                    context,
                                    "Solicitud enviada",
                                    Toast.LENGTH_LONG
                                ).show()

                            } else {

                                Toast.makeText(
                                    context,
                                    "Ya existe una solicitud pendiente",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            repeat(120) {

                                delay(1000)

                                val estado = try {

                                    withContext(Dispatchers.IO) {
                                        EntradasRepository.obtenerEstadoSolicitud(
                                            token,
                                            idCliente
                                        )
                                    }

                                } catch (e: Exception) {

                                    Toast.makeText(
                                        context,
                                        "Error consultando la solicitud",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return@launch
                                }

                                Log.d("SOLICITUD", "Estado: $estado")

                                when (estado?.trim()) {

                                    "PENDIENTE",
                                    "EN_REVISION" -> {
                                        // Sigue esperando
                                    }

                                    "ACEPTADA" -> {

                                        Toast.makeText(
                                            context,
                                            "✅ Entrada aprobada. ¡Bienvenido!",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        return@launch
                                    }

                                    "RECHAZADA" -> {

                                        Toast.makeText(
                                            context,
                                            "❌ Solicitud rechazada",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        return@launch
                                    }

                                    else -> {

                                        Toast.makeText(
                                            context,
                                            "Estado desconocido: $estado",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        return@launch
                                    }
                                }
                            }

                            Toast.makeText(
                                context,
                                "La solicitud expiró.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@launch
                        }

                        val ok = withContext(Dispatchers.IO) {
                            EntradasRepository.registrarEntrada(
                                token,
                                idCliente
                            )
                        }

                        Toast.makeText(
                            context,
                            if (ok)
                                "Entrada registrada correctamente"
                            else
                                "Error registrando entrada",
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: Exception) {

                        Log.e("SUPABASE", "ERROR", e)

                        Toast.makeText(
                            context,
                            "No fue posible registrar la entrada",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }

            } else {

                Log.d("QR", "ESCANEO CANCELADO")
            }
        }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Button(
                onClick = {
                    val hoy = LocalDate.now()

                    val fechaVencimiento = try {
                        LocalDate.parse(fechaPago)
                    } catch (e: Exception) {
                        null
                    }

                    if (fechaVencimiento != null && fechaVencimiento.isBefore(hoy)) {

                        mostrarDialogo = true
                        return@Button
                    }


                    val options = ScanOptions().apply {
                        setPrompt("Escanea el código QR")
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setBeepEnabled(true)
                        setOrientationLocked(true)
                        captureActivity = PortraitCaptureActivity::class.java
                    }
                    scanLauncher.launch(options)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Escanear código QR", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "PULSA PARA REGISTRAR TU ENTRADA",
                color = TextoSecundario,
                style = MaterialTheme.typography.labelSmall
            )

            if (mostrarDialogo) {

                AlertDialog(
                    onDismissRequest = {
                        mostrarDialogo = false
                    },
                    title = {
                        Text("Membresía vencida")
                    },
                    text = {
                        Text(
                            "Tu membresía se encuentra vencida.\n\n" +
                                    "Para registrar tu entrada debes realizar el pago en la administración del gimnasio."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                mostrarDialogo = false
                            }
                        ) {
                            Text("Entendido")
                        }
                    }
                )
            }
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
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home"
                    )
                },
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
                icon = {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = "Plans"
                    )
                },
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
                icon = {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "TRAINER"
                    )
                },
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
                icon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile"
                    )
                },
                label = { Text("PROFILE") }
            )
        }
    }

    //////////////////////////////////////////////////////////
// 👀 PREVIEW
//////////////////////////////////////////////////////////
    @RequiresApi(Build.VERSION_CODES.O)
    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun mostrar() {
        GymApplicationTheme {
            HomeScreen(navController = rememberNavController())
        }
    }
