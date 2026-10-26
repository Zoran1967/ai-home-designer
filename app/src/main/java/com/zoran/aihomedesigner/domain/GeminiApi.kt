package com.zoran.aihomedesigner.domain

/** Rezultat poziva servisa za slike: slika (base64) ili razlog neuspeha. */
sealed interface GeminiResult {
    data class Image(val mimeType: String, val base64: String) : GeminiResult

    data class Failure(val kind: Kind, val detail: String?) : GeminiResult

    enum class Kind {
        /** Servis je vratio grešku (loš ključ, nepoznat model, ...). [Failure.detail] je poruka servisa. */
        HTTP_ERROR,

        /** Previše zahteva ili je potrošena kvota (HTTP 429). */
        RATE_LIMITED,

        /** Servis je odbio zahtev iz bezbednosnih razloga. [Failure.detail] je razlog. */
        BLOCKED,

        /** Odgovor je uspešan, ali nema sliku. */
        NO_IMAGE
    }
}

/**
 * Pravljenje zahteva i čitanje odgovora Gemini API-ja (generateContent). Bez Androida i bez biblioteka za JSON,
 * pa se lako testira. Odgovor sa slikom može da ima nekoliko megabajta, zato se čita običnim traženjem u
 * tekstu, a ne izrazima koji bi se na tolikom tekstu srušili.
 */
object GeminiApi {

    /** Podrazumevano ime modela za slike. Imena modela se često menjaju, pa se u aplikaciji može izmeniti. */
    const val DEFAULT_MODEL = "gemini-nano-banana-2.1"

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    /** Ime modela sme da ima samo slova, cifre, tačku, crticu i podvlaku. */
    fun isValidModelName(model: String): Boolean = Regex("[A-Za-z0-9._-]{1,80}").matches(model.trim())

    fun endpoint(model: String): String = "$BASE_URL${model.trim()}:generateContent"

    /** Telo zahteva: jedan tekst i traženi izlaz u vidu slike. Ključ ide u zaglavlje, ne u telo. */
    fun requestBody(prompt: String): String =
        "{\"contents\":[{\"parts\":[{\"text\":${jsonString(prompt)}}]}]," +
            "\"generationConfig\":{\"responseModalities\":[\"TEXT\",\"IMAGE\"]}}"

    fun jsonString(text: String): String {
        val sb = StringBuilder("\"")
        for (c in text) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append("\\u").append(String.format("%04x", c.code))
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }

    fun parseResponse(httpStatus: Int, body: String): GeminiResult {
        if (httpStatus !in 200..299) {
            val message = stringValue(body, "message", 0)
            val kind = if (httpStatus == 429) GeminiResult.Kind.RATE_LIMITED else GeminiResult.Kind.HTTP_ERROR
            return GeminiResult.Failure(kind, message ?: "HTTP $httpStatus")
        }

        val inline = Regex("\"(?:inlineData|inline_data)\"\\s*:\\s*\\{").find(body)
        if (inline != null) {
            val from = inline.range.last + 1
            val mime = stringValue(body, "mimeType", from) ?: stringValue(body, "mime_type", from)
            val data = stringValue(body, "data", from)
            if (mime != null && mime.startsWith("image/") && !data.isNullOrEmpty()) {
                return GeminiResult.Image(mime, data)
            }
        }

        val blocked = stringValue(body, "blockReason", 0)
        if (blocked != null) return GeminiResult.Failure(GeminiResult.Kind.BLOCKED, blocked)
        return GeminiResult.Failure(GeminiResult.Kind.NO_IMAGE, null)
    }

    /** Vrednost prvog JSON teksta sa ključem [key] posle položaja [from], ili null. */
    private fun stringValue(body: String, key: String, from: Int): String? {
        val needle = "\"$key\""
        var at = body.indexOf(needle, from)
        while (at >= 0) {
            var i = at + needle.length
            while (i < body.length && body[i].isWhitespace()) i++
            if (i < body.length && body[i] == ':') {
                i++
                while (i < body.length && body[i].isWhitespace()) i++
                if (i < body.length && body[i] == '"') return readString(body, i + 1)
            }
            at = body.indexOf(needle, at + needle.length)
        }
        return null
    }

    private fun readString(body: String, start: Int): String? {
        var end = start
        var hasEscape = false
        while (end < body.length) {
            val c = body[end]
            if (c == '\\') { hasEscape = true; end += 2; continue }
            if (c == '"') break
            end++
        }
        if (end >= body.length) return null
        val raw = body.substring(start, end)
        return if (hasEscape) unescape(raw) else raw
    }

    private fun unescape(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '\\' || i + 1 >= raw.length) { sb.append(c); i++; continue }
            when (val e = raw[i + 1]) {
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                't' -> sb.append('\t')
                'b' -> sb.append('\b')
                'f' -> sb.append('\u000C')
                'u' -> {
                    val hex = if (i + 6 <= raw.length) raw.substring(i + 2, i + 6) else ""
                    val code = hex.toIntOrNull(16)
                    if (code != null) { sb.append(code.toChar()); i += 6; continue }
                    sb.append('u')
                }
                else -> sb.append(e) // \" \\ \/
            }
            i += 2
        }
        return sb.toString()
    }
}
