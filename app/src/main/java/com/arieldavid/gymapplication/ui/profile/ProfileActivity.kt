package com.arieldavid.gymapplication.ui.profile

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Serializable
data class Cliente(

    @SerialName("id_clientes")
    val uuid: String,

    val nombre: String,
    val telefono: String? = null,
    val id_membresia: Int? = null,
    val numero_cliente: Int? = null,
    val email: String? = null,
    val Payment: List<Payment> = emptyList()
)

@Serializable
data class Payment(
    val fecha_pago: String? = null,
    val fecha_sig_pago: String? = null
)

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

        val token = prefs.getString(
            "access_token",
            null
        )

        val userId = prefs.getString(
            "user_id",
            null
        )


        Log.d("PERFIL_DEBUG", "USER ID: $userId")
        Log.d("PERFIL_DEBUG", "TOKEN: $token")

        if (token == null || userId == null) {

            navController.navigate("login") {
                popUpTo(0)
            }

            return@LaunchedEffect
        }

        try {

            val json = PerfilRepository.cargarPerfil(token, userId)

            Log.d("PERFIL_DEBUG", "JSON: $json")

            if (!json.isNullOrEmpty()) {

                val parsed = Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<List<Cliente>>(json)

                val cliente = parsed.firstOrNull()

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
                                        fechaSigPago = cliente.Payment
                                        .firstOrNull()
                                    ?.fecha_sig_pago
                            )
                        )

                        Log.d("PERFIL_DEBUG", "GUARDADO EN ROOM")
                    }
                    prefs.edit()
                        .putString("nombre", cliente.nombre)
                        .apply()

                    perfil = cliente
                }

            } else {

                Log.e("PERFIL_DEBUG", "JSON NULL -> CARGANDO ROOM")

                val local = withContext(Dispatchers.IO) {
                    dao.getProfile()
                }

                Log.d("PERFIL_DEBUG", "LOCAL ROOM: $local")

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

                    Log.d("PERFIL_DEBUG", "PERFIL OFFLINE CARGADO")

                } else {

                    errorCarga = true
                }
            }

        } catch (e: Exception) {

            Log.e("PERFIL_DEBUG", "ERROR GENERAL: ${e.message}")

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

            } else {

                errorCarga = true
            }

        } finally {

            loading = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .padding(20.dp),

        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            text = "MI PERFIL",
            color = TextoPrincipal,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        when {

            loading -> {

                CircularProgressIndicator(
                    color = Color.White
                )
            }

            errorCarga -> {

                Text(
                    text = "Error cargando perfil",
                    color = Color.Red
                )
            }

            perfil != null -> {

                perfil?.let { cliente ->

                    ProfileItem(
                        "Nombre",
                        cliente.nombre
                    )

                    ProfileItem(
                        "Teléfono",
                        cliente.telefono ?: "-"
                    )

                    ProfileItem(
                        "Email",
                        cliente.email ?: "-"
                    )

                    ProfileItem(
                        "Número cliente",
                        cliente.numero_cliente?.toString() ?: "-"
                    )

                    ProfileItem("Fecha de Pago", cliente.Payment.firstOrNull()?.fecha_sig_pago ?: "-"
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

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

@Composable
fun ProfileItem(
    titulo: String,
    valor: String
) {

    Card(

        colors = CardDefaults.cardColors(
            containerColor = Cards
        ),

        shape = RoundedCornerShape(12.dp)
    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            horizontalArrangement =
            Arrangement.SpaceBetween
        ) {

            Text(
                titulo,
                color = TextoSecundario
            )

            Text(
                valor,
                color = TextoPrincipal,
                fontWeight = FontWeight.Bold
            )
        }
    }
}