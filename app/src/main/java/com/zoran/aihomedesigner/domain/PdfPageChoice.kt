package com.zoran.aihomedesigner.domain

/** Izbor strane PDF-a koju korisnik upisuje (brojanje od 1, kako je upisuje korisnik). */
object PdfPageChoice {

    /**
     * Pretvara upisani tekst u broj strane. Vraća null ako tekst nije ceo broj od 1 do [pageCount].
     * Razmaci oko broja se zanemaruju.
     */
    fun parse(text: String, pageCount: Int): Int? {
        val page = text.trim().toIntOrNull() ?: return null
        return page.takeIf { it in 1..pageCount }
    }
}
