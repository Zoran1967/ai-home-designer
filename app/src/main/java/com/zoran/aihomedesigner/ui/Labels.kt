package com.zoran.aihomedesigner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.domain.DimensionError
import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.ProjectNameError
import com.zoran.aihomedesigner.domain.ProjectValidator
import com.zoran.aihomedesigner.domain.PropertyType
import com.zoran.aihomedesigner.domain.RoomNameError
import com.zoran.aihomedesigner.domain.RoomValidator

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

@Composable
fun roomNameErrorText(error: RoomNameError): String = when (error) {
    RoomNameError.EMPTY -> stringResource(R.string.room_error_name_empty)
    RoomNameError.TOO_LONG ->
        stringResource(R.string.room_error_name_too_long, RoomValidator.MAX_NAME_LENGTH)
    RoomNameError.DUPLICATE -> stringResource(R.string.room_error_name_duplicate)
}

@Composable
fun dimensionErrorText(error: DimensionError, minM: Double, maxM: Double): String = when (error) {
    DimensionError.MISSING -> stringResource(R.string.error_dim_missing)
    DimensionError.NOT_A_NUMBER -> stringResource(R.string.error_dim_not_number)
    DimensionError.TOO_SMALL -> stringResource(R.string.error_dim_too_small, formatMeters(minM))
    DimensionError.TOO_LARGE -> stringResource(R.string.error_dim_too_large, formatMeters(maxM))
}

@Composable
fun dimensionSourceLabel(source: DimensionSource): String = when (source) {
    DimensionSource.USER_CONFIRMED -> stringResource(R.string.source_user_confirmed)
    DimensionSource.IMPORTED_CALIBRATED -> stringResource(R.string.source_imported)
    DimensionSource.DETECTED_UNCONFIRMED -> stringResource(R.string.source_detected)
    DimensionSource.ESTIMATED -> stringResource(R.string.source_estimated)
}
