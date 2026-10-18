package com.zoran.aihomedesigner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Red u tabeli "projects". Vrsta nekretnine se čuva kao tekst (ime), ne kao redni broj. */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val createdAtMillis: Long
)
