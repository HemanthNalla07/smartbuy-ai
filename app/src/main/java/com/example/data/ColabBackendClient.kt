package com.example.data

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class BackendPingResult(
  val success: Boolean,
  val message: String,
)

object ColabBackendClient {
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(6, TimeUnit.SECONDS)
      .readTimeout(6, TimeUnit.SECONDS)
      .build()

  suspend fun pingHealth(baseUrl: String): BackendPingResult =
    withContext(Dispatchers.IO) {
      val cleanUrl = baseUrl.trim().trimEnd('/')
      if (cleanUrl.isEmpty()) {
        return@withContext BackendPingResult(false, "Enter your Colab ngrok or Cloud Run URL first.")
      }
      if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
        return@withContext BackendPingResult(false, "URL must start with http:// or https://")
      }
      try {
        val request = Request.Builder().url("$cleanUrl/health").get().build()
        client.newCall(request).execute().use { response ->
          val body = response.body?.string().orEmpty()
          if (response.isSuccessful) {
            BackendPingResult(true, "Connected (${response.code}): $body")
          } else {
            BackendPingResult(false, "HTTP ${response.code}: ${response.message}")
          }
        }
      } catch (e: Exception) {
        BackendPingResult(false, "Connection failed: ${e.localizedMessage ?: "Unreachable"}")
      }
    }
}
