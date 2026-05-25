import android.content.Context
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.arieldavid.gymapplication.AuthClass
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {



    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

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
                    error = false // 👈 limpia error al escribir
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
                },
                placeholder = { Text("••••••••") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
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
            }

            if (error) {
                Text(
                    text = "Invalid email or password",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        try {

                            val success = AuthClass.login(context, email, password)
                            if (success) {

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
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                error = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            error = true
                        }
                        loading = false
                    }
                },
                    enabled = !loading, // 👈 evita spam
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
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