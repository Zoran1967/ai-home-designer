package com.zoran.aihomedesigner

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.util.Date

/**
 * Pomoć pri pronalaženju grešaka kad nemamo računar: kad se aplikacija neočekivano zatvori,
 * tehnički opis greške se upiše u fajl, a pri sledećem otvaranju se prikaže na ekranu
 * da se može kopirati. Opis ne sadrži ličnih podataka i ne napušta telefon dok ga korisnik ne pošalje.
 */
object CrashReporter {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val text = buildString {
                    appendLine("Vreme: ${Date()}")
                    appendLine("Nit: ${thread.name}")
                    appendLine(
                        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
                            "${Build.MANUFACTURER} ${Build.MODEL}"
                    )
                    appendLine()
                    append(Log.getStackTraceString(throwable))
                }
                File(appContext.filesDir, FILE_NAME).writeText(text)
            } catch (_: Throwable) {
                // Ako ni upis ne uspe, nema šta više da se uradi.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Vraća opis poslednje greške ili null ako je nema. */
    fun readPending(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return file.readText().takeIf { it.isNotBlank() }
    }

    fun clear(context: Context) {
        File(context.applicationContext.filesDir, FILE_NAME).delete()
    }
}
