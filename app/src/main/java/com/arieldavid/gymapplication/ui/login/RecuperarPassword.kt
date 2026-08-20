
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.arieldavid.gymapplication.AuthClass
import com.arieldavid.gymapplication.ui.colores.MainBackground
import com.arieldavid.gymapplication.ui.colores.TextoPrincipal
import com.arieldavid.gymapplication.ui.colores.TextoSecundario

@Composable
fun RecuperarPasswordScreen(
    navController: NavController
) {

    var email by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .padding(20.dp)
    ) {

        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "RECUPERAR CONTRASEÑA",
            color = TextoPrincipal,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Introduce el correo asociado a tu cuenta y te enviaremos un enlace para restablecer tu contraseña.",
            color = TextoSecundario
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "EMAIL",
            color = TextoSecundario
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            placeholder = {
                Text("ejemplo@gmail.com")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {

                scope.launch {

                    loading = true

                    val enviado =
                        AuthClass.recuperarPassword(
                            email
                        )

                    loading = false

                    if (enviado) {

                        Toast.makeText(
                            context,
                            "Se ha enviado un correo de recuperación",
                            Toast.LENGTH_LONG
                        ).show()

                        navController.popBackStack()

                    } else {

                        Toast.makeText(
                            context,
                            "Error al enviar el correo",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray
            ),
            shape = RoundedCornerShape(4.dp)
        ) {

            Text(
                if (loading) "ENVIANDO..." else "ENVIAR CORREO",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        ) {

            Text(
                "Volver al inicio de sesión",
                color = TextoSecundario
            )
        }
    }
}
@Composable
fun NuevaPasswordScreen(
    navController: NavController
) {

    var password by remember {
        mutableStateOf("")
    }

    var confirmar by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            "Nueva contraseña",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Nueva contraseña")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = confirmar,
            onValueChange = {
                confirmar = it
            },
            label = {
                Text("Confirmar contraseña")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(24.dp)
        )

        Button(
            onClick = {

                if (password != confirmar) {

                    Toast.makeText(
                        context,
                        "Las contraseñas no coinciden",
                        Toast.LENGTH_LONG
                    ).show()

                    return@Button
                }

                scope.launch {

                    loading = true

                    val ok =
                        AuthClass.cambiarPassword(
                            context,
                            password
                        )

                    loading = false

                    if (ok) {

                        Toast.makeText(
                            context,
                            "Contraseña actualizada",
                            Toast.LENGTH_LONG
                        ).show()

                        navController.navigate("login") {
                            popUpTo(0)
                        }

                    } else {

                        Toast.makeText(
                            context,
                            "Error al actualizar",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            enabled = !loading
        ) {

            Text(
                if (loading)
                    "GUARDANDO..."
                else
                    "CAMBIAR CONTRASEÑA"
            )
        }
    }
}