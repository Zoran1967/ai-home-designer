package com.zoran.aihomedesigner.ui

// Ovaj ekran služi i za pravljenje nove sobe (initialRoom = null) i za izmenu postojeće.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.data.RoomSaveResult
import com.zoran.aihomedesigner.domain.DimensionLimits
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.RoomInputErrors

@Composable
fun RoomFormScreen(
    initialRoom: Room?,
    onSubmit: (RoomInput, (RoomSaveResult) -> Unit) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val key = initialRoom?.id
    var name by rememberSaveable(key) { mutableStateOf(initialRoom?.name ?: "") }
    var length by rememberSaveable(key) {
        mutableStateOf(initialRoom?.let { metersToInputText(it.lengthM) } ?: "")
    }
    var width by rememberSaveable(key) {
        mutableStateOf(initialRoom?.let { metersToInputText(it.widthM) } ?: "")
    }
    var height by rememberSaveable(key) {
        mutableStateOf(initialRoom?.let { metersToInputText(it.heightM) } ?: "")
    }
    var thickness by rememberSaveable(key) {
        mutableStateOf(initialRoom?.wallThicknessM?.let { metersToInputText(it) } ?: "")
    }
    var errors by remember { mutableStateOf<RoomInputErrors?>(null) }
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
                    if (initialRoom == null) R.string.create_room_title else R.string.edit_room_title
                ),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            FormField(
                label = stringResource(R.string.field_room_name),
                value = name,
                onValueChange = {
                    name = it
                    errors = errors?.copy(name = null)
                },
                errorText = errors?.name?.let { roomNameErrorText(it) },
                hint = stringResource(R.string.hint_room_name),
                numeric = false
            )
            Spacer(modifier = Modifier.height(8.dp))

            FormField(
                label = stringResource(R.string.field_room_length),
                value = length,
                onValueChange = {
                    length = it
                    errors = errors?.copy(length = null)
                },
                errorText = errors?.length?.let {
                    dimensionErrorText(it, DimensionLimits.MIN_LENGTH_M, DimensionLimits.MAX_LENGTH_M)
                },
                hint = stringResource(R.string.hint_room_length),
                numeric = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            FormField(
                label = stringResource(R.string.field_room_width),
                value = width,
                onValueChange = {
                    width = it
                    errors = errors?.copy(width = null)
                },
                errorText = errors?.width?.let {
                    dimensionErrorText(it, DimensionLimits.MIN_LENGTH_M, DimensionLimits.MAX_LENGTH_M)
                },
                hint = stringResource(R.string.hint_room_width),
                numeric = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            FormField(
                label = stringResource(R.string.field_room_height),
                value = height,
                onValueChange = {
                    height = it
                    errors = errors?.copy(height = null)
                },
                errorText = errors?.height?.let {
                    dimensionErrorText(it, DimensionLimits.MIN_HEIGHT_M, DimensionLimits.MAX_HEIGHT_M)
                },
                hint = stringResource(R.string.hint_room_height),
                numeric = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            FormField(
                label = stringResource(R.string.field_room_thickness),
                value = thickness,
                onValueChange = {
                    thickness = it
                    errors = errors?.copy(wallThickness = null)
                },
                errorText = errors?.wallThickness?.let {
                    dimensionErrorText(it, DimensionLimits.MIN_THICKNESS_M, DimensionLimits.MAX_THICKNESS_M)
                },
                hint = stringResource(R.string.hint_room_thickness),
                numeric = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        val input = RoomInput(name, length, width, height, thickness)
                        onSubmit(input) { result ->
                            saving = false
                            when (result) {
                                is RoomSaveResult.Success -> onDone()
                                is RoomSaveResult.Invalid -> errors = result.errors
                                RoomSaveResult.NotFound -> onDone()
                            }
                        }
                    }
                ) { Text(stringResource(R.string.save)) }

                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun FormField(
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
