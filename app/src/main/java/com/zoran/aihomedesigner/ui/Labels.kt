package com.zoran.aihomedesigner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.domain.ProjectNameError
import com.zoran.aihomedesigner.domain.ProjectValidator
import com.zoran.aihomedesigner.domain.PropertyType

@Composable
fun propertyTypeLabel(type: PropertyType): String = when (type) {
    PropertyType.APARTMENT -> stringResource(R.string.type_apartment)
    PropertyType.HOUSE -> stringResource(R.string.type_house)
}

@Composable
fun nameErrorText(error: ProjectNameError): String = when (error) {
    ProjectNameError.EMPTY -> stringResource(R.string.error_name_empty)
    ProjectNameError.TOO_LONG ->
        stringResource(R.string.error_name_too_long, ProjectValidator.MAX_NAME_LENGTH)
    ProjectNameError.DUPLICATE -> stringResource(R.string.error_name_duplicate)
}
