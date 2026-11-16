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
    ): JSONObject = withContext(Dispatchers.IO) {

        val url = URL(
            "$PROJECT_URL/auth/v1/token?grant_type=password"
        )

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
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
                throw Exception(
                    JSONObject(response).optString(
                        "msg",
                        JSONObject(response).optString(
                            "message",
                            "Login gagal"
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
