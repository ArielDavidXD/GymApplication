
package com.arieldavid.gymapplication.ui.home
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.arieldavid.gymapplication.local.QrLocal.LocalApi
import com.arieldavid.gymapplication.local.QrLocal.LocalQrRepository
import com.arieldavid.gymapplication.local.QrLocal.NetworkScanner
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
import java.util.Date
import java.util.Locale

// Refresco automatico del Home
private const val INTERVALO_REFRESCO_MS = 30_000L     // consulta cada 30 segundos
private const val INTERVALO_ESCANEO_MS = 5 * 60_000L  // escaneo de red completo: cada 5 minutos

// True si el telefono esta en la WiFi del gimnasio: ahi esta la PC con la API local
private fun enRedLocal(context: Context): Boolean {

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false

    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

// 🔥 HOME
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current




    var fechaPago by remember {
        mutableStateOf("-")
    }

    // Hora en que se pudieron actualizar los datos por ultima vez
    var ultimaActualizacion by remember {
        mutableStateOf<String?>(null)
    }

    // Cuando se escaneo la red por ultima vez (para no escanear cada pocos segundos)
    var ultimoEscaneo by remember {
        mutableStateOf(0L)
    }

    // Nombre que muestra el saludo (se actualiza con el refresco)
    var nombreUI by remember {
        mutableStateOf(
            context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("nombre", null) ?: "Usuario"
        )
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
        val linkedId = prefs.getString("linked_client_id", null)

        Log.d("HOME_DBG", "token=$token userId=$userId linked=$linkedId fechaPago=$fechaPago")

        if (token == null || userId == null) {

            if (linkedId != null) {
                try {
                    val pago = LocalApi.getService().obtenerPago(linkedId)
                    val dao = DatabaseProvider.getDatabase(context).clientDAO()
                    val actual = dao.getByUUID(linkedId)

                    if (actual != null && pago.fecha_sig_pago != null) {
                        dao.insert(actual.copy(fechaSigPago = pago.fecha_sig_pago))
                        fechaPago = pago.fecha_sig_pago
                    }
                } catch (e: Exception) {
                    Log.d("HOME", "Sin conexión al servidor local, uso datos de Room")
                }
                return@LaunchedEffect
            }
            if (linkedId != null || fechaPago != "-") {
                Log.d("HOME", "Modo vinculado/offline (linked=$linkedId)")
                return@LaunchedEffect
            }

            Log.d("HOME_DBG", "REDIRIGIENDO A LOGIN desde Home")

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

    //////////////////////////////////////////////////////
    // REFRESCO PERIODICO
    // Trae los datos del cliente (pago, nombre, plan, etc.)
    // mientras el Home este abierto:
    // 1) Si hay WiFi y responde la PC del gimnasio, usa la API local
    //    (es la base de datos mas actualizada).
    // 2) Si no, usa Supabase.
    // Devuelve true si pudo actualizar algo.
    //////////////////////////////////////////////////////

    // Guarda los datos del cliente en Room y en el saludo.
    // Actualiza valores; NO toca el esquema de la tabla.
    suspend fun guardarCliente(
        uuid: String,
        nombre: String?,
        email: String?,
        idMembresia: Int?,
        idEntrenador: String?,
        numeroCliente: Int?
    ) {

        if (uuid.isBlank()) return

        val dao = DatabaseProvider.getDatabase(context).clientDAO()

        val actual = dao.getByUUID(uuid) ?: ClientEntity(uuid = uuid)

        dao.insert(
            actual.copy(
                // El nombre nunca se borra (dato critico para mostrar)
                nombre = nombre?.takeIf { it.isNotBlank() } ?: actual.nombre,
                // Email y entrenador: null = el cliente no tiene (se borra)
                email = email?.takeIf { it.isNotBlank() },
                idEntrenador = idEntrenador
                    ?.takeIf { it.isNotBlank() && it != "null" },
                // Plan y numero: si no vino el dato, se respalda el anterior
                idMembresia = idMembresia?.takeIf { it > 0 } ?: actual.idMembresia,
                numeroCliente = numeroCliente?.takeIf { it > 0 } ?: actual.numeroCliente
            )
        )

        if (!nombre.isNullOrBlank() && nombre != nombreUI) {

            nombreUI = nombre

            context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .edit()
                .putString("nombre", nombre)
                .apply()
        }
    }

    suspend fun refrescarDatos(): Boolean {

        var actualizado = false

        if (enRedLocal(context)) {

            // IP guardada que responde: rapido (1 pedido)
            var ip = NetworkScanner.probarGuardada(context)

            if (ip != null) {

                SpringRepository.actualizarIp(ip)
                LocalApi.actualizarIp(ip)

            } else if (System.currentTimeMillis() - ultimoEscaneo >= INTERVALO_ESCANEO_MS) {

                // Escaneo completo de la subred: solo cada 5 minutos
                ultimoEscaneo = System.currentTimeMillis()

                if (NetworkScanner.asegurarServidor(context)) {
                    ip = "servidor"
                }
            }

            if (ip != null) {

                val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                val idCliente = prefs.getString("linked_client_id", null)
                    ?: DatabaseProvider.getDatabase(context).clientDAO().getProfile()?.uuid

                if (idCliente != null) {

                    // Proximo pago
                    try {

                        val pago = LocalApi.getService().obtenerPago(idCliente)
                        val fecha = pago.fecha_sig_pago

                        if (!fecha.isNullOrBlank()) {

                            val dao = DatabaseProvider.getDatabase(context).clientDAO()
                            val actual = dao.getByUUID(idCliente)

                            if (actual != null) dao.insert(actual.copy(fechaSigPago = fecha))

                            fechaPago = fecha
                            actualizado = true
                        }

                    } catch (e: Exception) {
                        Log.d("HOME", "Pago local no disponible: ${e.message}")
                    }

                    // Nombre y demas datos del cliente
                    try {

                        val cli = LocalApi.getService().obtenerCliente(idCliente)

                        guardarCliente(
                            uuid = cli.idClientes,
                            nombre = cli.nombre,
                            email = cli.email,
                            idMembresia = cli.id_membresia,
                            idEntrenador = cli.id_entrenador,
                            numeroCliente = cli.numeroCliente
                        )

                        actualizado = true

                    } catch (e: Exception) {
                        Log.d("HOME", "Cliente local no disponible: ${e.message}")
                    }
                }
            }
        }

        // Sin servidor local: Supabase (si hay sesion iniciada).
        // Solo si la local no actualizo nada: lo local es mas fresco
        // y Supabase podria pisar datos nuevos con datos de hace 20 s.
        if (!actualizado) {

            val token = AuthClass.getAccessToken(context)
            val userId = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("user_id", null)

            if (token != null && userId != null) {

                try {

                    val fecha = repository.obtenerFechaPago(context, token, userId)

                    if (fecha != "-" && fecha.isNotBlank()) {
                        fechaPago = fecha
                        actualizado = true
                    }

                } catch (e: Exception) {
                    Log.e("HOME", "Error refrescando pago: ${e.message}")
                }

                try {

                    val cli = repository.obtenerClienteOnline(token, userId)

                    if (cli != null) {

                        guardarCliente(
                            uuid = cli.optString("id_clientes"),
                            nombre = cli.optString("nombre"),
                            email = cli.optString("email"),
                            idMembresia = cli.optInt("id_membresia", -1).takeIf { it > 0 },
                            idEntrenador = cli.optString("id_entrenador")
                                .takeIf { it.isNotBlank() && it != "null" },
                            numeroCliente = cli.optInt("numero_cliente", -1).takeIf { it > 0 }
                        )

                        actualizado = true
                    }

                } catch (e: Exception) {
                    Log.e("HOME", "Error refrescando cliente: ${e.message}")
                }
            }
        }

        return actualizado
    }

    // Cada pocos segundos: si pudo actualizar, guarda la hora
    LaunchedEffect(Unit) {

        while (true) {

            delay(INTERVALO_REFRESCO_MS)

            val actualizado = try {
                refrescarDatos()
            } catch (e: Exception) {
                Log.e("HOME", "Error refrescando: ${e.message}")
                false
            }

            if (actualizado) {
                ultimaActualizacion =
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            }
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

        Greeting(nombreUI)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (ultimaActualizacion == null) "Sin actualizar aún"
            else "Última actualización: $ultimaActualizacion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
fun Greeting(nombre: String) {

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

                        // Asegura que el servidor esté configurado (IP guardada
                        // o búsqueda automática en la subred). Si no hay servidor
                        // local, lanzamos para ir al respaldo de Supabase.
                        if (!NetworkScanner.asegurarServidor(context)) {
                            throw Exception("Sin servidor local")
                        }

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

                        val resultado = withContext(Dispatchers.IO) {
                            EntradasRepository.registrarEntrada(
                                token,
                                idCliente
                            )
                        }

                        Toast.makeText(
                            context,
                            resultado.mensaje,
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
