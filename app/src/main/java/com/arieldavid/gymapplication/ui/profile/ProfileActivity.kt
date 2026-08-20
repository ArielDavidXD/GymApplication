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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Badge
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
    val telefono: String? = null,
    val id_membresia: Int? = null,
    val numero_cliente: Int? = null,
    val email: String? = null,
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

            perfil = Cliente(
                uuid = local.uuid,
                nombre = local.nombre ?: "",
                telefono = local.telefono,
                id_membresia = local.idMembresia,
                numero_cliente = local.numeroCliente,
                email = local.email,
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
                                telefono = cliente.telefono,
                                idMembresia = cliente.id_membresia,
                                numeroCliente = cliente.numero_cliente,
                                email = cliente.email,
                                fechaSigPago =
                                cliente.Payment
                                    .firstOrNull()
                                    ?.fecha_sig_pago
                            )
                        )
                    }

                    prefs.edit()
                        .putString(
                            "nombre",
                            cliente.nombre
                        )
                        .apply()

                    perfil = cliente
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
                    Icons.Default.Phone,
                    "Teléfono",
                    cliente.telefono ?: "-"
                )

                HorizontalDivider()

                InfoRow(
                    Icons.Default.Email,
                    "Correo",
                    cliente.email ?: "-"
                )

                HorizontalDivider()

                InfoRow(
                    Icons.Default.Badge,
                    "Número de Cliente",
                    cliente.numero_cliente?.toString() ?: "-"
                )
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

