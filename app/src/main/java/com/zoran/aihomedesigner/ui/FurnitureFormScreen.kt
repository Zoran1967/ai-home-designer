package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.data.FurnitureSaveResult
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureCategory
import com.zoran.aihomedesigner.domain.FurnitureInput
import com.zoran.aihomedesigner.domain.FurnitureInputErrors
import com.zoran.aihomedesigner.domain.FurnitureLimits
import com.zoran.aihomedesigner.domain.FurnitureNameError
import com.zoran.aihomedesigner.domain.Room

/** Ekran za dodavanje (initialFurniture = null) ili izmenu predmeta nameštaja. */
@Composable
fun FurnitureFormScreen(
    room: Room,
    initialFurniture: Furniture?,
    onSubmit: (FurnitureInput, (FurnitureSaveResult) -> Unit) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val key = initialFurniture?.id
    var name by rememberSaveable(key) { mutableStateOf(initialFurniture?.name ?: "") }
    var category by rememberSaveable(key) {
        mutableStateOf(initialFurniture?.category ?: FurnitureCategory.OTHER)
    }
    var length by rememberSaveable(key) {
        mutableStateOf(initialFurniture?.let { metersToInputText(it.lengthM) } ?: "")
    }
    var width by rememberSaveable(key) {
        mutableStateOf(initialFurniture?.let { metersToInputText(it.widthM) } ?: "")
    }
    var height by rememberSaveable(key) {
        mutableStateOf(initialFurniture?.let { metersToInputText(it.heightM) } ?: "")
    }
    var rotation by rememberSaveable(key) { mutableIntStateOf(initialFurniture?.rotationDeg ?: 0) }
    var errors by remember { mutableStateOf<FurnitureInputErrors?>(null) }
    var saving by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(
                    if (initialFurniture == null) R.string.create_furniture_title else R.string.edit_furniture_title
                ),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(
                    R.string.furniture_room_info,
                    formatMeters(room.lengthM),
                    formatMeters(room.widthM),
                    formatMeters(room.heightM)
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            FurnitureField(
                label = stringResource(R.string.field_furniture_name),
                value = name,
                onValueChange = {
                    name = it
                    errors = errors?.copy(name = null)
                },
                errorText = errors?.name?.let { furnitureNameErrorText(it) },
                hint = stringResource(R.string.hint_furniture_name),
                numeric = false
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.label_furniture_category),
                style = MaterialTheme.typography.titleMedium
            )
            FurnitureCategory.values().forEach { option ->
                ChoiceRow(
                    selected = category == option,
                    label = furnitureCategoryLabel(option),
                    onClick = { category = option }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            FurnitureField(
                label = stringResource(R.string.field_furniture_length),
                value = length,
                onValueChange = {
                    length = it
                    errors = errors?.copy(length = null)
                },
                errorText = errors?.length?.let {
                    dimensionErrorText(it, FurnitureLimits.MIN_SIZE_M, FurnitureLimits.MAX_SIZE_M)
                },
                hint = stringResource(R.string.hint_furniture_length),
                numeric = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            FurnitureField(
                label = stringResource(R.string.field_furniture_width),
                value = width,
                onValueChange = {
                    width = it
                    errors = errors?.copy(width = null)
                },
                errorText = errors?.width?.let {
                    dimensionErrorText(it, FurnitureLimits.MIN_SIZE_M, FurnitureLimits.MAX_SIZE_M)
                },
                hint = stringResource(R.string.hint_furniture_width),
                numeric = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            FurnitureField(
                label = stringResource(R.string.field_furniture_height),
                value = height,
                onValueChange = {
                    height = it
                    errors = errors?.copy(height = null)
                },
                errorText = errors?.height?.let {
                    dimensionErrorText(it, FurnitureLimits.MIN_HEIGHT_M, FurnitureLimits.MAX_HEIGHT_M)
                },
                hint = stringResource(R.string.hint_furniture_height),
                numeric = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.label_furniture_rotation),
                style = MaterialTheme.typography.titleMedium
            )
            listOf(0, 90, 180, 270).forEach { option ->
                ChoiceRow(
                    selected = rotation == option,
                    label = stringResource(R.string.furniture_rotation_option, option),
                    onClick = { rotation = option }
                )
            }
            Text(
                text = stringResource(R.string.furniture_rotation_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        val input = FurnitureInput(name, category, length, width, height, rotation)
                        onSubmit(input) { result ->
                            saving = false
                            when (result) {
                                is FurnitureSaveResult.Success -> onDone()
                                is FurnitureSaveResult.Invalid -> errors = result.errors
                                FurnitureSaveResult.NotFound -> onDone()
                            }
                        }
                    }
                ) { Text(stringResource(R.string.save)) }

                OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
private fun furnitureNameErrorText(error: FurnitureNameError): String = when (error) {
    FurnitureNameError.EMPTY -> stringResource(R.string.furniture_error_name_empty)
    FurnitureNameError.TOO_LONG ->
        stringResource(R.string.furniture_error_name_too_long, FurnitureLimits.MAX_NAME_LENGTH)
}

@Composable
private fun ChoiceRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
private fun FurnitureField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String?,
    hint: String,
    numeric: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = errorText != null,
        supportingText = { Text(errorText ?: hint) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
