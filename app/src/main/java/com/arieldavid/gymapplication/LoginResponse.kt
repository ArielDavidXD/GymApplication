package com.arieldavid.gymapplication

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val expires_at: Long? = null,
    val refresh_token: String,
    val user: User? = null
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val aud: String? = null,
    val role: String? = null,
    val email_confirmed_at: String? = null
)



