import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.arieldavid.gymapplication.local.QrLocal.LocalApi
import com.arieldavid.gymapplication.local.QrLocal.NetworkScanner
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.ClientEntity
import com.arieldavid.gymapplication.local.entity.MembresiaEntity
import com.arieldavid.gymapplication.local.entity.TrainerEntity
import com.arieldavid.gymapplication.ui.colores.MainBackground
import com.arieldavid.gymapplication.ui.colores.TextoPrincipal
import com.arieldavid.gymapplication.ui.colores.TextoSecundario
import com.arieldavid.gymapplication.ui.home.PortraitCaptureActivity
import com.arieldavid.gymapplication.ui.profile.idDesdeEnumTurno
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

// True si el telefono tiene WiFi conectada. Se revisan TODAS las redes,
// igual que en Home: si la WiFi no esta validada, Android puede usar datos
// moviles como red activa y nosotros igual queremos la red del gimnasio.
private fun hayWifi(context: Context): Boolean {

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return cm.allNetworks.any { red ->
        cm.getNetworkCapabilities(red)?.let { caps ->
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } == true
    }
}

@Composable
fun LoginScreen(
    navController: NavController,
    servidorListo: Boolean
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scannerLauncher =
        rememberLauncherForActivityResult(
            contract = ScanContract()
        ) { result ->

            val contenido = result.contents

            if (contenido == null) {

                Toast.makeText(
                    context,
                    "Escaneo cancelado",
                    Toast.LENGTH_SHORT
                ).show()

                return@rememberLauncherForActivityResult
            }

            scope.launch {

                try {

                    // =========================
                    // 1. VALIDAR QR
                    // =========================

                    val partes = contenido.split("|")

                    if (
                        partes.size != 3 ||
                        partes[0] != "ALTAPINTA" ||
                        partes[1] != "VINCULACION"
                    ) {

                        Toast.makeText(
                            context,
                            "QR de vinculación no válido",
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }

                    val idCliente = partes[2]

                    // =========================
                    // 2. PEDIR CLIENTE AL SERVIDOR
                    // =========================

                    val cliente = LocalApi
                        .getService()
                        .obtenerCliente(idCliente)

                    // =========================
                    // 3. GUARDAR EN ROOM
                    // =========================

                    val entity = ClientEntity(
                        uuid = cliente.idClientes,
                        nombre = cliente.nombre,
                        numeroCliente = cliente.numeroCliente,
                        email = cliente.email,
                        idMembresia = cliente.id_membresia,
                        idEntrenador = cliente.id_entrenador,
                        // El API manda el turno como "turno_7_8", etc.
                        idTurno = idDesdeEnumTurno(cliente.turno ?: "")
                    )

                    val database = DatabaseProvider.getDatabase(context)

                    database.clientDAO().deleteAll()
                    database.clientDAO().insert(entity)
// =========================
// 4. GUARDAR CLIENTE VINCULADO
// =========================

                    context
                        .getSharedPreferences("auth", Context.MODE_PRIVATE)
                        .edit()
                        .putString("linked_client_id", cliente.idClientes)
                        .putString("id_clientes", cliente.idClientes)
                        .putString("nombre", cliente.nombre ?: "Usuario")
                        .apply()

// =========================
// 5. SINCRONIZAR PLANES Y ENTRENADORES
// =========================

                    var sincronizado = true

                    try {
                        val api = LocalApi.getService()

                        val entrenadores = api.obtenerEntrenadores()
                        val membresias = api.obtenerMembresias()

                        val trainersDao = database.TrainersDAO()
                        trainersDao.deleteAll()
                        entrenadores.forEach {
                            trainersDao.insert(
                                TrainerEntity(
                                    id_entrenadores = it.id_entrenadores,
                                    nombre = it.nombre,
                                    descripcion = it.descripcion
                                )
                            )
                        }

                        database.MembresiaDAO().insertAll(
                            membresias.map {
                                MembresiaEntity(
                                    id_membresia = it.id_membresia,
                                    nombre = it.nombre,
                                    precio_normal = it.precio_normal,
                                    precio_transferencia = it.precio_transferencia,
                                    beneficio = it.beneficio
                                )
                            }
                        )

                        Log.d("VINCULAR", "Sync OK: ${entrenadores.size} entrenadores, ${membresias.size} membresías")

                    } catch (e: Exception) {
                        sincronizado = false
                        Log.e("VINCULAR", "Cliente vinculado, pero falló la sincronización", e)
                    }
                    try {
                        val pago = LocalApi.getService().obtenerPago(cliente.idClientes)

                        if (pago.fecha_sig_pago != null) {
                            database.clientDAO().insert(
                                entity.copy(fechaSigPago = pago.fecha_sig_pago)
                            )
                        }

                        Log.d("VINCULAR", "Pago OK: ${pago.fecha_sig_pago} (${pago.estado})")

                    } catch (e: Exception) {
                        Log.e("VINCULAR", "No se pudo obtener el pago", e)
                    }

// =========================
// 6. ENTRAR A HOME
// =========================

                    Toast.makeText(
                        context,
                        if (sincronizado) "Cliente vinculado correctamente"
                        else "Vinculado, pero no se pudieron descargar planes y entrenadores",
                        Toast.LENGTH_LONG
                    ).show()

                    navController.navigate("home") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }

                } catch (e: Exception) {

                    e.printStackTrace()

                    Toast.makeText(
                        context,
                        "No se pudo vincular el cliente: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .padding(20.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ALTAPINTA GYM",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "BIENVENIDO",
                color = TextoPrincipal,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Para iniciar tu sesión en el gimnasio acércate a recepción y escanea el código QR de vinculación.",
                color = TextoSecundario,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {

                    // Sin WiFi no hay servidor local: mandamos al menu de
                    // WiFi del telefono para que se conecte al del gimnasio.
                    if (!hayWifi(context)) {

                        Toast.makeText(
                            context,
                            "Conectate al Wi-Fi del gimnasio para vincularte",
                            Toast.LENGTH_LONG
                        ).show()

                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (e: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }

                        return@Button
                    }

                    scope.launch {

                        // Busca el servidor automáticamente (IP guardada o
                        // escaneo de la subred) y lo deja configurado.
                        val ok = NetworkScanner.asegurarServidor(context)

                        if (!ok) {

                            Toast.makeText(
                                context,
                                "No se encontró el servidor del gimnasio. Verifica que estés conectado al Wi-Fi del gimnasio e inténtalo de nuevo.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@launch
                        }

                        val options = ScanOptions().apply {
                            setPrompt("Escanea el QR de vinculación de AltaPinta")
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setBeepEnabled(true)
                            setOrientationLocked(true)
                            captureActivity = PortraitCaptureActivity::class.java
                        }

                        scannerLauncher.launch(options)
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                ),

                shape = RoundedCornerShape(4.dp)

            ) {

                Text(
                    text = "VINCULAR CLIENTE",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "El escaneo se realiza en recepción, conectado a la red Wi-Fi del gimnasio.",
                color = TextoSecundario,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}