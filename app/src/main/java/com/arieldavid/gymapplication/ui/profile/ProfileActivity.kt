package com.arieldavid.gymapplication.ui.profile

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arieldavid.gymapplication.AuthClass
import com.arieldavid.gymapplication.local.QrLocal.NetworkScanner
import com.arieldavid.gymapplication.local.QrLocal.SpringRepository
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.ClientEntity
import com.arieldavid.gymapplication.ui.PerfilRepository
import com.arieldavid.gymapplication.ui.colores.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow

@Serializable
data class Cliente(

    @SerialName("id_clientes")
    val uuid: String,

    val nombre: String,
    val id_membresia: Int? = null,
    val numero_cliente: Int? = null,
    val email: String? = null,
    val id_entrenador: String? = null,
    val id_turno: Int? = null,
    val plan: String? = null,
    val entrenador: String? = null,
    val Payment: List<Payment> = emptyList(),
    val eliminado: Boolean = false
) {

}

@Serializable
data class Payment(
    val fecha_pago: String? = null,
    val fecha_sig_pago: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileScreen(navController: NavController) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var mostrarDialogo by remember {
        mutableStateOf(false)
    }

    var nuevaPassword by remember {
        mutableStateOf("")
    }

    var perfil by remember {
        mutableStateOf<Cliente?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var errorCarga by remember {
        mutableStateOf(false)
    }


    val db = DatabaseProvider.getDatabase(context)
    val dao = db.clientDAO()


    LaunchedEffect(Unit) {

        val prefs = context.getSharedPreferences(
            "auth",
            Context.MODE_PRIVATE
        )

        //////////////////////////////////////////////////////
        // 1. CARGAR ROOM SIEMPRE
        //////////////////////////////////////////////////////

        val local = withContext(Dispatchers.IO) {
            dao.getProfile()
        }

        if (local != null) {

            val planNombre = withContext(Dispatchers.IO) {
                db.MembresiaDAO()
                    .getAll()
                    .firstOrNull { it.id_membresia == local.idMembresia }
                    ?.nombre
            }

            val entrenadorNombre = withContext(Dispatchers.IO) {
                db.TrainersDAO()
                    .getAll()
                    .firstOrNull { it.id_entrenadores == local.idEntrenador }
                    ?.nombre
            }

            perfil = Cliente(
                uuid = local.uuid,
                nombre = local.nombre ?: "",
                id_membresia = local.idMembresia,
                numero_cliente = local.numeroCliente,
                email = local.email,
                id_entrenador = local.idEntrenador,
                id_turno = local.idTurno,
                plan = planNombre,
                entrenador = entrenadorNombre,
                Payment = listOf(
                    Payment(
                        fecha_sig_pago = local.fechaSigPago
                    )
                )
            )

            loading = false
        }

        //////////////////////////////////////////////////////
        // 2. TOKEN
        //////////////////////////////////////////////////////

        val token = AuthClass.getAccessToken(context)

        val userId = prefs.getString(
            "user_id",
            null
        )

        Log.d("PERFIL_DEBUG", "TOKEN: $token")
        Log.d("PERFIL_DEBUG", "USER: $userId")

        //////////////////////////////////////////////////////
        // 3. SIN INTERNET -> USAR ROOM
        //////////////////////////////////////////////////////

        if (token == null || userId == null) {

            if (perfil != null) {

                Log.d("PERFIL_DEBUG", "Modo offline")

                loading = false
                return@LaunchedEffect
            }

            errorCarga = true
            loading = false
            return@LaunchedEffect
        }

        //////////////////////////////////////////////////////
        // 4. SINCRONIZAR ONLINE
        //////////////////////////////////////////////////////

        try {

            val json =
                PerfilRepository.cargarPerfil(
                    token,
                    userId
                )

            Log.d(
                "PERFIL_DEBUG",
                "JSON: $json"
            )

            if (!json.isNullOrEmpty()) {

                val parsed =
                    Json {
                        ignoreUnknownKeys = true
                    }.decodeFromString<List<Cliente>>(json)

                val cliente =
                    parsed.firstOrNull()

                if (cliente != null) {

                    withContext(Dispatchers.IO) {

                        dao.deleteAll()

                        dao.insert(
                            ClientEntity(
                                uuid = cliente.uuid,
                                nombre = cliente.nombre,
                                idMembresia = cliente.id_membresia,
                                numeroCliente = cliente.numero_cliente,
                                email = cliente.email,
                                idEntrenador = cliente.id_entrenador,
                                idTurno = cliente.id_turno,
                                fechaSigPago =
                                cliente.Payment
                                    .firstOrNull()
                                    ?.fecha_sig_pago
                            )
                        )
                    }

                    // Resuelve el nombre del plan y del entrenador
                    // desde las tablas locales.
                    val planNombre = withContext(Dispatchers.IO) {
                        db.MembresiaDAO()
                            .getAll()
                            .firstOrNull { it.id_membresia == cliente.id_membresia }
                            ?.nombre
                    }

                    val entrenadorNombre = withContext(Dispatchers.IO) {
                        db.TrainersDAO()
                            .getAll()
                            .firstOrNull { it.id_entrenadores == cliente.id_entrenador }
                            ?.nombre
                    }

                    prefs.edit()
                        .putString(
                            "nombre",
                            cliente.nombre
                        )
                        .apply()

                    perfil = cliente.copy(
                        plan = planNombre,
                        entrenador = entrenadorNombre
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                "PERFIL_DEBUG",
                "Error sincronizando: ${e.message}"
            )

            if (perfil == null) {
                errorCarga = true
            }
        

        } finally {

            loading = false
        }

        ///////////////////////////////////////////////////////
        // 5. REFRESCO LOCAL: SI EL CELULAR ESTA EN LA MISMA RED
        //    DEL GIMNASIO, EL ESCRITORIO TIENE LOS DATOS MAS FRESCOS
        //    (se actualiza solo cuando hay un cambio: plan, turno,
        //     entrenador, pago, etc.)
        ///////////////////////////////////////////////////////

        try {

            // OJO: el API local busca por id_clientes (no por user_id,
            // que es el UUID de auth de Supabase).
            val idClienteLocal = prefs.getString("id_clientes", null)
                ?: prefs.getString("linked_client_id", null)

            if (idClienteLocal != null &&
                NetworkScanner.asegurarServidor(context)
            ) {

                val cliLocal = withContext(Dispatchers.IO) {
                    SpringRepository.obtenerCliente(idClienteLocal)
                }

                if (cliLocal != null) {

                    val pagoLocal = withContext(Dispatchers.IO) {
                        SpringRepository.ultimoPago(idClienteLocal)
                    }

                    val idTurnoLocal =
                        idDesdeEnumTurno(cliLocal.optString("turno", ""))

                    Log.d(
                        "PERFIL_DEBUG",
                        "LOCAL: idCliente=$idClienteLocal"
                                + " turno='${cliLocal.optString("turno", "")}'"
                                + " -> idTurno=$idTurnoLocal"
                                + " entrenador=${cliLocal.optString("nombreEntrenador", "")}"
                    )

                    val fechaSig = pagoLocal
                        ?.optString("fecha_sig_pago", "")
                        ?.takeIf { it.isNotBlank() && it != "null" }

                    // Datos que ya tenía Room: sirven de respaldo para no
                    // borrar el turno/pago si el servidor no los mandó.
                    val roomActual = withContext(Dispatchers.IO) {
                        dao.getProfile()
                    }

                    val entity = ClientEntity(
                        uuid = cliLocal.optString(
                            "idClientes",
                            idClienteLocal
                        ),
                        nombre = cliLocal.optString("nombre", ""),
                        email = cliLocal
                            .optString("email", "")
                            .takeIf {
                                it.isNotBlank() && it != "null"
                            }
                            ?: roomActual?.email,
                        idMembresia = cliLocal
                            .optInt("id_membresia", -1)
                            .takeIf { it > 0 },
                        numeroCliente = cliLocal
                            .optInt("numeroCliente", -1)
                            .takeIf { it > 0 },
                        idEntrenador = cliLocal
                            .optString("id_entrenador", "")
                            .takeIf { it.isNotBlank() && it != "null" },
                        idTurno = idTurnoLocal ?: roomActual?.idTurno,
                        fechaSigPago = fechaSig
                            ?: roomActual?.fechaSigPago
                    )

                    withContext(Dispatchers.IO) {

                        dao.deleteAll()
                        dao.insert(entity)
                    }

                    val planNombre = withContext(Dispatchers.IO) {
                        db.MembresiaDAO()
                            .getAll()
                            .firstOrNull {
                                it.id_membresia == entity.idMembresia
                            }
                            ?.nombre
                    } ?: cliLocal
                        .optString("nombreMembresia", "")
                        .takeIf { it.isNotBlank() && it != "null" }

                    val entrenadorNombre = withContext(Dispatchers.IO) {
                        db.TrainersDAO()
                            .getAll()
                            .firstOrNull {
                                it.id_entrenadores == entity.idEntrenador
                            }
                            ?.nombre
                    } ?: cliLocal
                        .optString("nombreEntrenador", "")
                        .takeIf { it.isNotBlank() && it != "null" }

                    perfil = Cliente(
                        uuid = entity.uuid,
                        nombre = entity.nombre ?: "",
                        id_membresia = entity.idMembresia,
                        numero_cliente = entity.numeroCliente,
                        email = entity.email,
                        id_entrenador = entity.idEntrenador,
                        id_turno = entity.idTurno,
                        plan = planNombre,
                        entrenador = entrenadorNombre,
                        Payment = listOf(
                            Payment(
                                fecha_sig_pago = entity.fechaSigPago
                            )
                        )
                    )

                    loading = false
                    errorCarga = false

                    Log.d(
                        "PERFIL_DEBUG",
                        "Perfil refrescado desde el servidor local"
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                "PERFIL_DEBUG",
                "Refresco local no disponible: ${e.message}"
            )
        }
    }
    when {

        loading -> {

            CircularProgressIndicator()

        }

        errorCarga -> {

            Text("Error")

        }

        perfil != null -> {
            perfil?.let { cliente ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MainBackground)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),

                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Surface(
                            modifier = Modifier
                                .size(110.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                ),
                            shape = CircleShape,
                            color = Color(0xFF4CAF50)
                        ) {

                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = cliente.nombre,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(50)
                        ) {

                            Text(
                                text = "Cliente #${cliente.numero_cliente}",
                                color = Color.White,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 6.dp
                                ),
                                style = MaterialTheme.typography.labelLarge
                            )

                        }

                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    MembershipCard(
                        cliente.Payment.firstOrNull()?.fecha_sig_pago ?: "-"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ProfileInfoCard(cliente)

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(

                        onClick = {
                            mostrarDialogo = true
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {

                        Text("CAMBIAR CONTRASEÑA")
                    }

                    Button(

                        onClick = {

                            scope.launch {

                                dao.deleteAll()

                                val prefs = context.getSharedPreferences(
                                    "auth",
                                    Context.MODE_PRIVATE
                                )

                                prefs.edit()
                                    .clear()
                                    .apply()

                                navController.navigate("login") {
                                    popUpTo(0)
                                }
                            }
                        },

                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {

                        Text(
                            "CERRAR SESIÓN",
                            color = Color.White
                        )
                    }

                }
            }
        }

    }

    if (mostrarDialogo) {

        AlertDialog(

            onDismissRequest = {
                mostrarDialogo = false
            },

            title = {
                Text("Nueva contraseña")
            },

            text = {

                OutlinedTextField(
                    value = nuevaPassword,

                    onValueChange = {
                        nuevaPassword = it
                    },

                    visualTransformation =
                    PasswordVisualTransformation()
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        mostrarDialogo = false

                        if (nuevaPassword.length < 6) {

                            Toast.makeText(
                                context,
                                "Mínimo 6 caracteres",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@TextButton
                        }

                        scope.launch {

                            val ok = AuthClass
                                .cambiarPassword(
                                    context,
                                    nuevaPassword
                                )

                            Toast.makeText(
                                context,
                                if (ok)
                                    "Contraseña actualizada"
                                else
                                    "Error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {

                    Text("Guardar")
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MembershipCard(
    fechaPago: String
) {

    val hoy = java.time.LocalDate.now()

    val fecha = try {
        java.time.LocalDate.parse(fechaPago)
    } catch (e: Exception) {
        null
    }

    val diasRestantes = fecha?.let {
        java.time.temporal.ChronoUnit.DAYS.between(hoy, it)
    } ?: 999

    val (titulo, colorEstado) = when {

        diasRestantes < 0 ->
            "🔴 Membresía vencida" to Color(0xFFD32F2F)

        diasRestantes <= 5 ->
            "🟠 Próxima a vencer" to Color(0xFFFF9800)

        else ->
            "🟢 Membresía activa" to Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Cards
        ),
        shape = RoundedCornerShape(20.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = titulo,
                color = colorEstado,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Próximo pago",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatDate(fechaPago),
                color = TextoPrincipal,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (diasRestantes >= 0) {

                Text(
                    text = "Faltan $diasRestantes días",
                    color = colorEstado,
                    style = MaterialTheme.typography.bodyMedium
                )

            } else {

                Text(
                    text = "Pago pendiente",
                    color = colorEstado,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        }

    }
}
@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(date: String): String {

    return try {

        val formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val localDate =
            LocalDate.parse(date, formatter)

        localDate.format(

            DateTimeFormatter.ofPattern(
                "dd 'de' MMMM yyyy",
                Locale("es")
            )
        )

    } catch (e: Exception) {

        date

    }

}

    @Composable
    fun ProfileInfoCard(cliente: Cliente) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Cards
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Información Personal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )

                Spacer(modifier = Modifier.height(20.dp))

                InfoRow(
                    Icons.Default.Person,
                    "Nombre",
                    cliente.nombre
                )

                HorizontalDivider()

                InfoRow(
                    Icons.Default.Badge,
                    "Número de Cliente",
                    cliente.numero_cliente?.toString() ?: "-"
                )

                HorizontalDivider()

                InfoRow(
                    Icons.Default.CardMembership,
                    "Plan",
                    cliente.plan ?: "-"
                )

                if (cliente.entrenador != null) {

                    HorizontalDivider()

                    InfoRow(
                        Icons.Default.FitnessCenter,
                        "Entrenador",
                        cliente.entrenador!!
                    )
                }
            }
        }
    }


    @Composable
    fun InfoRow(
        icon: ImageVector,
        titulo: String,
        valor: String
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {

                Text(
                    text = titulo,
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = valor,
                    color = TextoPrincipal,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    /** Convierte el turno que manda el escritorio a su id.
     *
     *  El API manda el turno en dos formas posibles según
     *  la versión del servidor:
     *  - "Turno 14:00 - 15:00"   (toString del enum)
     *  - "turno_02_03"           (name del enum)
     *  Las dos se aceptan para que no dependa del jar.
     */
    fun idDesdeEnumTurno(valor: String): Int? {

        // ----------------------------------------------------
        // 1. FORMA "turno_02_03"
        // ----------------------------------------------------

        val porName = when (valor) {

            "turno_7_8" -> 1
            "turno_8_9" -> 2
            "turno_9_10" -> 3
            "turno_10_11" -> 4
            "turno_11_12" -> 5
            "turno_02_03" -> 6
            "turno_03_04" -> 7
            "turno_04_05" -> 8
            "turno_05_06" -> 9
            "turno_06_07" -> 10
            "turno_07_08" -> 11
            "turno_08_09" -> 12

            else -> null
        }

        if (porName != null) return porName

        // ----------------------------------------------------
        // 2. FORMA "Turno 14:00 - 15:00": nos quedamos con la
        //    hora de inicio. No hay turno de mañana y de tarde
        //    que empiecen a la misma hora, así no hay empate.
        // ----------------------------------------------------

        val hora = Regex("""(\d{1,2}):\d{2}""")
            .find(valor)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: return null

        return when (hora) {

            7 -> 1
            8 -> 2
            9 -> 3
            10 -> 4
            11 -> 5
            14 -> 6
            15 -> 7
            16 -> 8
            17 -> 9
            18 -> 10
            19 -> 11
            20 -> 12

            else -> null
        }
    }

