package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.Project
import com.zoran.aihomedesigner.domain.PropertyType

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    type = type.name,
    createdAtMillis = createdAtMillis
)

/** Vraća null ako je vrsta nekretnine u bazi nepoznata (npr. posle neispravne izmene). */
fun ProjectEntity.toDomainOrNull(): Project? {
    val propertyType = PropertyType.values().firstOrNull { it.name == type } ?: return null
    return Project(
        id = id,
        name = name,
        type = propertyType,
        createdAtMillis = createdAtMillis
    )
}
