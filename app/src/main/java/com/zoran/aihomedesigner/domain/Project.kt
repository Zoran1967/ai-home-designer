package com.zoran.aihomedesigner.domain

import java.util.UUID

/**
 * Projekat jedne nekretnine (stan ili kuća).
 * Sobe i mere se dodaju u kasnijim koracima.
 */
data class Project(
    val id: String,
    val name: String,
    val type: PropertyType,
    val createdAtMillis: Long
) {
    companion object {
        /** Stabilan, jedinstven identifikator projekta. */
        fun newId(): String = UUID.randomUUID().toString()
    }
}
