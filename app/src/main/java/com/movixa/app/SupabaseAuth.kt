
package com.movixa.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupabaseAuth {

    private const val PROJECT_URL = "https://bsbhcnpzajrrniywvlcb.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_iiMUfLXXA_g19qNnAldW1g_QJn_AMQT"

    suspend fun signIn(
        email: String,
        password: String
    ): JSONObject = request(
        "/auth/v1/token?grant_type=password",
        email,
        password
    )

    suspend fun signUp(
        email: String,
        password: String
    ): JSONObject = request(
        "/auth/v1/signup",
        email,
        password
    )

    private suspend fun request(
        path: String,
        email: String,
        password: String
    ): JSONObject = withContext(Dispatchers.IO) {

        val connection = URL("$PROJECT_URL$path")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            connection.setRequestProperty(
                "apikey", PUBLISHABLE_KEY
            )
            connection.setRequestProperty(
                "Content-Type", "application/json"
            )

            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            connection.outputStream.use {
                it.write(body.toString().toByteArray())
            }

            val responseCode = connection.responseCode

            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream.bufferedReader().use {
                it.readText()
            }

            if (responseCode !in 200..299) {
                val json = JSONObject(response)
                throw Exception(
                    json.optString(
                        "msg",
                        json.optString(
                            "message",
                            json.optString("error_description", "Permintaan gagal")
                        )
                    )
                )
            }

            JSONObject(response)

        } finally {
            connection.disconnect()
        }
    }
}
