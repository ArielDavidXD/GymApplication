import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.arieldavid.gymapplication.ui.colores.MainBackground
import com.arieldavid.gymapplication.ui.colores.TextoPrincipal
import com.arieldavid.gymapplication.ui.colores.TextoSecundario

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.arieldavid.gymapplication.AuthClass
import com.journeyapps.barcodescanner.CaptureActivity
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon

@Composable
fun LoginScreen(navController: NavController) {


    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var mostrarPassword by remember {
        mutableStateOf(false)
    }

    var error by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var usuarioEliminado by remember {
        mutableStateOf(false)
    }

    var recordar by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {

        val prefs = context.getSharedPreferences(
            "auth",
            Context.MODE_PRIVATE
        )

        val recordarUsuario =
            prefs.getBoolean("recordar", false)

        if (recordarUsuario) {

            email = prefs.getString(
                "saved_email",
                ""
            ) ?: ""

            password = prefs.getString(
                "saved_password",
                ""
            ) ?: ""

            recordar = true
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
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "ACCESS PERFORMANCE",
                color = TextoPrincipal,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your credentials",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 🔹 EMAIL
            Text("EMAIL", color = TextoSecundario)

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = false
                    usuarioEliminado = false
                },
                placeholder = { Text("example@email.com") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 PASSWORD
            Text("PASSWORD", color = TextoSecundario)

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = false
                    usuarioEliminado = false
                },

                placeholder = {
                    Text("••••••••")
                },

                modifier = Modifier.fillMaxWidth(),

                visualTransformation =
                if (mostrarPassword)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),

                trailingIcon = {

                    IconButton(
                        onClick = {
                            mostrarPassword = !mostrarPassword
                        }
                    ) {

                        Icon(
                            imageVector =
                            if (mostrarPassword)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = recordar,

                    onCheckedChange = {
                        recordar = it
                    }
                )

                Text(
                    "Recordarme",
                    color = Color.White
                )

//                TextButton(
//                    onClick = {
//                        navController.navigate("recuperar")
//                    }
//                ) {
//
//                    Text(
//                        "¿Olvidaste tu contraseña?"
//                    )
//                }
            }
            if (usuarioEliminado) {

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Su usuario ha sido eliminado. Para más información póngase en contacto con la administración del gimnasio.",
                    color = Color.Red,
                    fontSize = 15.sp
                )
            }

            if (error) {
                Text(
                    text = "Email o contraseña incorrecta",
                    color = Color.Red,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        try {

                            val result = AuthClass.login(
                                context,
                                email,
                                password
                            )

                            when (result) {

                                AuthClass.LoginResult.SUCCESS -> {

                                    val prefs = context.getSharedPreferences(
                                        "auth",
                                        Context.MODE_PRIVATE
                                    )

                                    if (recordar) {

                                        prefs.edit()
                                            .putBoolean("recordar", true)
                                            .putString("saved_email", email)
                                            .putString("saved_password", password)
                                            .apply()

                                    } else {

                                        prefs.edit()
                                            .remove("saved_email")
                                            .remove("saved_password")
                                            .putBoolean("recordar", false)
                                            .apply()
                                    }

                                    navController.navigate("home") {
                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                    }
                                }

                                AuthClass.LoginResult.INVALID_CREDENTIALS -> {

                                    error = true
                                }

                                AuthClass.LoginResult.ELIMINADO -> {

                                    usuarioEliminado = true

                                }
                            }

                        } catch (e: Exception) {

                            e.printStackTrace()

                            if (
                                e is UnknownHostException ||
                                e is ConnectException
                            ) {

                                Toast.makeText(
                                    context,
                                    "No hay conexión a internet",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                error = true
                            }
                        }

                        loading = false
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
                    if (loading) "LOADING..." else "LOGIN",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }


}