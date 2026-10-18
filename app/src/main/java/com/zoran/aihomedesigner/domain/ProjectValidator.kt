package com.zoran.aihomedesigner.domain

/** Moguće greške pri unosu imena projekta. Prikaz poruke radi korisnički ekran. */
enum class ProjectNameError {
    EMPTY,
    TOO_LONG,
    DUPLICATE
}

object ProjectValidator {

    const val MAX_NAME_LENGTH = 60

    /** Uklanja razmake sa početka i kraja i spaja višestruke razmake u jedan. */
    fun normalizeName(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ")

    /**
     * Proverava ime projekta.
     * Vraća null ako je ime ispravno, a inače razlog greške.
     */
    fun validateName(raw: String, existingNames: List<String>): ProjectNameError? {
        val name = normalizeName(raw)
        if (name.isEmpty()) return ProjectNameError.EMPTY
        if (name.length > MAX_NAME_LENGTH) return ProjectNameError.TOO_LONG
        val taken = existingNames.any { normalizeName(it).equals(name, ignoreCase = true) }
        if (taken) return ProjectNameError.DUPLICATE
        return null
    }
}
