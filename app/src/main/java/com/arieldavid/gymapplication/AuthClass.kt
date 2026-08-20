    package com.arieldavid.gymapplication
    
    import android.content.Context
    import android.util.Log
    import kotlinx.serialization.decodeFromString
    import kotlinx.serialization.json.Json
    import com.arieldavid.gymapplication.LoginResponse
    import com.arieldavid.gymapplication.ui.profile.Cliente
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.withContext
    import kotlinx.serialization.json.jsonObject
    import okhttp3.MediaType.Companion.toMediaType
    import okhttp3.OkHttpClient
    import okhttp3.Request
    import okhttp3.RequestBody.Companion.toRequestBody
    import java.net.ConnectException
    import java.net.UnknownHostException
    import android.util.Base64
    import kotlinx.serialization.json.jsonObject
    import kotlinx.serialization.json.jsonPrimitive
    
    object AuthClass {
    
        private val client = OkHttpClient()
        enum class LoginResult {
            SUCCESS,
            INVALID_CREDENTIALS,
            ELIMINADO
        }
    
        suspend fun signUp(context: Context, email: String, password: String): Boolean {
            val json = """{"email":"$email","password":"$password"}"""
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/auth/v1/signup")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
    
            val response = client.newCall(request).execute()
            val responseBody: String? = response.body?.string()
            if (responseBody == null || responseBody.length == 0) {
                return false
            }
    
            // Parsear y guardar token si lo devuelve
            val loginResponse = Json.decodeFromString<LoginResponse>(responseBody)
            val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            prefs.edit().putString("access_token", loginResponse.access_token).apply()
    
            return true
        }
    
        suspend fun login(
            context: Context,
            email: String,
            password: String
        ): LoginResult {
    
            return withContext(Dispatchers.IO) {
    
                Log.d("QR", "ENTRO LOGIN")
                val json = """{"email":"$email","password":"$password"}"""
    
                val body = json.toRequestBody(
                    "application/json".toMediaType()
                )
    
                val request = Request.Builder()
                    .url("${SupabaseConfig.URL}/auth/v1/token?grant_type=password")
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
    
                try {
    
                    Log.d("QR", "ANTES REQUEST")
                    val response = client.newCall(request).execute()
    
                    Log.d("QR", "DESPUES REQUEST")
    
                    val responseBody = response.body?.string()
    
                    if (responseBody.isNullOrEmpty())
                        return@withContext LoginResult.INVALID_CREDENTIALS
    
                    if (responseBody.contains("error"))
                        return@withContext LoginResult.INVALID_CREDENTIALS
    
                    val jsonParser = Json {
                        ignoreUnknownKeys = true
                    }
    
                    val loginResponse =
                        jsonParser.decodeFromString<LoginResponse>(
                            responseBody
                        )
    
                    val token = loginResponse.access_token
    
                    val userId =
                        loginResponse.user?.id ?: ""
    
    
                    //////////////////////////////////////////////////////
                    // BUSCAR PERFIL
                    //////////////////////////////////////////////////////
    
                    val perfilRequest = Request.Builder()
                        .url(
                            "${SupabaseConfig.URL}/rest/v1/Clientes?user_id=eq.$userId&select=*,Payment(*)"
                        )
                        .addHeader("apikey", SupabaseConfig.API_KEY)
                        .addHeader(
                            "Authorization",
                            "Bearer $token"
                        )
                        .addHeader(
                            "Content-Type",
                            "application/json"
                        )
                        .build()
    
                    val perfilResponse =
                        client.newCall(perfilRequest).execute()
    
                    val perfilBody =
                        perfilResponse.body?.string()
    
                    Log.d(
                        "QR",
                        "PERFIL: $perfilBody"
                    )
    
                    var nombre = "Usuario"
                    var idClientes = ""
    
                    if (!perfilBody.isNullOrEmpty()) {
    
                        val clientes =
                            jsonParser.decodeFromString<List<Cliente>>(
                                perfilBody
                            )
    
                        val cliente =
                            clientes.firstOrNull()
    
                        if (cliente?.eliminado == true) {
    
                            Log.d("LOGIN", "CLIENTE ELIMINADO")
    
                            return@withContext LoginResult.ELIMINADO
                        }
    
                        nombre =
                            cliente?.nombre ?: "Usuario"
    
                        // ESTE ES EL ID REAL DE LA TABLA CLIENTES
                        idClientes =
                            cliente?.uuid ?: ""
                        Log.d("QR", "UUID CLIENTE: $idClientes")
                    }
    
                    //////////////////////////////////////////////////////
                    // GUARDAR SESSION
                    //////////////////////////////////////////////////////
    
                    val prefs =
                        context.getSharedPreferences(
                            "auth",
                            Context.MODE_PRIVATE
                        )
    
                    prefs.edit()
                        .putString(
                            "access_token",
                            token,
    
    
                        )
                        .putString(
                            "refresh_token",
                            loginResponse.refresh_token
                        )
                        .putString(
                            "user_id",
                            userId
                        )
                        .putString(
                            "nombre",
                            nombre
                        )
                        .putString(
                            "id_clientes",
                            idClientes
                        )
    
                        .apply()
    
                    Log.d(
                        "QR",
                        "UUID GUARDADO PREFS: ${
                            prefs.getString("id_clientes", "VACIO")
                        }"
                    )
                    LoginResult.SUCCESS
    

                } catch (e: Exception) {
    
                    e.printStackTrace()
    
                    if(
                        e is UnknownHostException ||
                        e is ConnectException
                    ) {
                        throw e
                    }
    
                    LoginResult.INVALID_CREDENTIALS
                }
            }
        }
        suspend fun cambiarPassword(context: Context, nuevaPassword: String): Boolean {
            return withContext(Dispatchers.IO) {
                val token = getAccessToken(context)
                ?: return@withContext false
    
                val json = """{"password":"$nuevaPassword"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${SupabaseConfig.URL}/auth/v1/user")
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .put(body)
                    .build()
    
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    Log.d("AuthClass", "Respuesta cambio password: $responseBody")
    
                    response.isSuccessful
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }
        suspend fun recuperarPassword(
            email: String
        ): Boolean {
    
            return withContext(Dispatchers.IO) {
    
                try {
    
                    val json = """
                    {
                      "email":"$email",
                      "redirectTo":"gymapp://reset-password"
                    }
                """.trimIndent()
    
                    val body = json.toRequestBody(
                        "application/json".toMediaType()
                    )
    
                    val request = Request.Builder()
                        .url("${SupabaseConfig.URL}/auth/v1/recover")
                        .addHeader(
                            "apikey",
                            SupabaseConfig.API_KEY
                        )
                        .addHeader(
                            "Content-Type",
                            "application/json"
                        )
                        .post(body)
                        .build()
    
                    val response =
                        client.newCall(request).execute()
    
                    Log.d("RECOVERY", "CODE = ${response.code}")
                    Log.d(
                        "RECOVERY",
                        response.body?.string() ?: "sin respuesta"
                    )
    
                    response.isSuccessful
    
                } catch (e: Exception) {
    
                    e.printStackTrace()
                    false
                }
            }
        }
        suspend fun refreshToken(
            context: Context
        ): Boolean {
    
            return withContext(Dispatchers.IO) {
    
                try {
    
                    val prefs = context.getSharedPreferences(
                        "auth",
                        Context.MODE_PRIVATE
                    )
    
                    val refreshToken =
                        prefs.getString(
                            "refresh_token",
                            null
                        ) ?: return@withContext false
    
                    val json = """
                    {
                        "refresh_token":"$refreshToken"
                    }
                """.trimIndent()
    
                    val body = json.toRequestBody(
                        "application/json".toMediaType()
                    )
    
                    val request = Request.Builder()
                        .url("${SupabaseConfig.URL}/auth/v1/token?grant_type=refresh_token")
                        .addHeader("apikey", SupabaseConfig.API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build()
    
                    val response =
                        client.newCall(request).execute()
    
                    if (!response.isSuccessful)
                        return@withContext false
    
                    val responseBody =
                        response.body?.string()
                            ?: return@withContext false
    
                    val loginResponse =
                        Json {
                            ignoreUnknownKeys = true
                        }.decodeFromString<LoginResponse>(
                            responseBody
                        )
    
                    prefs.edit()
                        .putString(
                            "access_token",
                            loginResponse.access_token
                        )
                        .putString(
                            "refresh_token",
                            loginResponse.refresh_token
                        )
                        .apply()
    
                    Log.d("AUTH", "Token renovado correctamente")
    
                    true
    
                } catch (e: Exception) {
    
                    e.printStackTrace()
                    false
                }
            }
        }
    
        fun tokenValido(context: Context): Boolean {
    
            val prefs = context.getSharedPreferences(
                "auth",
                Context.MODE_PRIVATE
            )
    
            val token = prefs.getString(
                "access_token",
                null
            ) ?: return false
    
            return try {
    
                val payload = token.split(".")[1]
    
                val decoded = String(
                    Base64.decode(
                        payload,
                        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                    )
                )
    
                val exp = Json
                    .parseToJsonElement(decoded)
                    .jsonObject["exp"]
                    ?.jsonPrimitive
                    ?.content
                    ?.toLong()
                    ?: return false
    
                val ahora =
                    System.currentTimeMillis() / 1000
    
                ahora < exp
    
            } catch (e: Exception) {
    
                false
            }
        }

        suspend fun getAccessToken(context: Context): String? {

            val prefs = context.getSharedPreferences(
                "auth",
                Context.MODE_PRIVATE
            )

            val token = prefs.getString(
                "access_token",
                null
            ) ?: return null

            // Si todavía sirve
            if (tokenValido(context))
                return token

            // Intentar renovarlo
            return if (refreshToken(context)) {

                prefs.getString(
                    "access_token",
                    token
                )

            } else {

                Log.d(
                    "AUTH",
                    "Sin internet, usando token almacenado"
                )

                // DEVOLVER EL TOKEN VIEJO
                token
            }
        }
    
    
    
    }
