package com.zoran.aihomedesigner.ai

import android.content.Context
import com.zoran.aihomedesigner.domain.GeminiApi
import com.zoran.aihomedesigner.domain.GeminiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Podešavanja za AI ilustraciju: ključ i ime modela. Čuvaju se u fajlovima u `noBackupFilesDir`, koji ne ulazi
 * u automatsku kopiju podataka (backup), pa ključ ne odlazi na Google Drive. Ključ se ne šifruje.
 */
object AiSettings {

    private const val KEY_FILE = "ai_gemini_key.txt"
    private const val MODEL_FILE = "ai_gemini_model.txt"

    fun apiKey(context: Context): String = read(context, KEY_FILE)

    fun saveApiKey(context: Context, key: String) = write(context, KEY_FILE, key.trim())

    fun clearApiKey(context: Context) {
        File(context.noBackupFilesDir, KEY_FILE).delete()
    }

    fun model(context: Context): String = read(context, MODEL_FILE).ifBlank { GeminiApi.DEFAULT_MODEL }

    fun saveModel(context: Context, model: String) = write(context, MODEL_FILE, model.trim())

    /** Ključ ima samo vidljive ASCII znakove, bez razmaka i novih redova (ide u zaglavlje zahteva). */
    fun isPlausibleKey(key: String): Boolean {
        val trimmed = key.trim()
        return trimmed.length >= 10 && trimmed.all { it.code in 33..126 }
    }

    private fun read(context: Context, name: String): String =
        try {
            val file = File(context.noBackupFilesDir, name)
            if (file.exists()) file.readText(Charsets.UTF_8).trim() else ""
        } catch (e: IOException) {
            ""
        }

    private fun write(context: Context, name: String, value: String) {
        try {
            File(context.noBackupFilesDir, name).writeText(value, Charsets.UTF_8)
        } catch (e: IOException) {
            // Bez upisa se ključ samo ne pamti do sledećeg puta; slanje i dalje radi.
        }
    }
}

/** Poziv Gemini API-ja običnim HTTPS zahtevom, bez dodatnih biblioteka. */
object GeminiClient {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 180_000
    private const val MAX_RESPONSE_BYTES = 40 * 1024 * 1024

    /** Baca [IOException] kad nema veze sa servisom ili je odgovor prevelik. */
    suspend fun generate(apiKey: String, model: String, prompt: String): GeminiResult =
        withContext(Dispatchers.IO) {
            val connection = URL(GeminiApi.endpoint(model)).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("x-goog-api-key", apiKey)
                connection.outputStream.use { it.write(GeminiApi.requestBody(prompt).toByteArray(Charsets.UTF_8)) }

                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.use { readLimited(it) }.orEmpty()
                GeminiApi.parseResponse(status, body)
            } finally {
                connection.disconnect()
            }
        }

    private fun readLimited(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (out.size() + count > MAX_RESPONSE_BYTES) throw IOException("Odgovor je prevelik")
            out.write(buffer, 0, count)
        }
        return out.toString("UTF-8")
    }
}
