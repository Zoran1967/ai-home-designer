package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zoran.aihomedesigner.domain.DrawAxis
import com.zoran.aihomedesigner.domain.DrawnRect
import com.zoran.aihomedesigner.domain.ImageRescale
import com.zoran.aihomedesigner.domain.MeasureParse
import com.zoran.aihomedesigner.domain.MeasureParser
import com.zoran.aihomedesigner.domain.RoomDrawing
import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.RoomInputErrors

/**
 * Prozor za sobu nacrtanu preko slike plana. Dužina i širina su predložene sa slike, a korisnik ih sme da
 * promeni. Ime i visina zida moraju da se upišu. Ako se upisana mera dosta razlikuje od slike, prikazuje se
 * upozorenje (ne zabrana). Mera koju korisnik upiše je mera sobe.
 */
@Composable
fun DrawRoomDialog(
    rect: DrawnRect,
    onSubmit: (RoomInput, (RoomSaveResult) -> Unit) -> Unit,
    onRescale: (Double, (Boolean) -> Unit) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    // Mere sa slike; posle podešavanja razmere slike one se skaliraju faktorom
    var currentRect by remember(rect) { mutableStateOf(rect) }
    // 0 = nije pokušano, 1 = razmera podešena, 2 = podešavanje odbijeno
    var rescaleState by remember { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var length by rememberSaveable { mutableStateOf(metersToInputText(rect.lengthM)) }
    var width by rememberSaveable { mutableStateOf(metersToInputText(rect.widthM)) }
    var height by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf<RoomInputErrors?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun typed(text: String): Double? = (
        MeasureParser.parse(text, DimensionLimits.MIN_LENGTH_M, DimensionLimits.MAX_LENGTH_M) as? MeasureParse.Value
        )?.meters

    val mismatches = RoomDrawing.mismatches(currentRect, typed(length), typed(width))

    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = { Text(stringResource(R.string.draw_room_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.draw_room_note),
                    style = MaterialTheme.typography.bodySmall
                )
                DrawRoomField(
                    label = stringResource(R.string.draw_room_field_name),
                    value = name,
                    onValueChange = {
                        name = it
                        errors = errors?.copy(name = null)
                    },
                    errorText = errors?.name?.let { roomNameErrorText(it) },
                    hint = null,
                    decimal = false
                )
                DrawRoomField(
                    label = stringResource(R.string.draw_room_field_length),
                    value = length,
                    onValueChange = {
                        length = it
                        errors = errors?.copy(length = null)
                    },
                    errorText = errors?.length?.let {
                        dimensionErrorText(it, DimensionLimits.MIN_LENGTH_M, DimensionLimits.MAX_LENGTH_M)
                    },
                    hint = null,
                    decimal = true
                )
                DrawRoomField(
                    label = stringResource(R.string.draw_room_field_width),
                    value = width,
                    onValueChange = {
                        width = it
                        errors = errors?.copy(width = null)
                    },
                    errorText = errors?.width?.let {
                        dimensionErrorText(it, DimensionLimits.MIN_LENGTH_M, DimensionLimits.MAX_LENGTH_M)
                    },
                    hint = null,
                    decimal = true
                )
                DrawRoomField(
                    label = stringResource(R.string.draw_room_field_height),
                    value = height,
                    onValueChange = {
                        height = it
                        errors = errors?.copy(height = null)
                    },
                    errorText = errors?.height?.let {
                        dimensionErrorText(it, DimensionLimits.MIN_HEIGHT_M, DimensionLimits.MAX_HEIGHT_M)
                    },
                    hint = stringResource(R.string.draw_room_hint_height),
                    decimal = true
                )
                mismatches.forEach { mismatch ->
                    val factor = ImageRescale.factor(mismatch.drawnM, mismatch.typedM)
                    Text(
                        text = stringResource(
                            if (mismatch.axis == DrawAxis.LENGTH) {
                                R.string.draw_room_mismatch_length
                            } else {
                                R.string.draw_room_mismatch_width
                            },
                            formatMeters(mismatch.drawnM),
                            formatMeters(mismatch.typedM)
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (factor != null) {
                        OutlinedButton(
                            onClick = {
                                onRescale(factor) { ok ->
                                    if (ok) {
                                        currentRect = ImageRescale.scaleRect(currentRect, factor)
                                        rescaleState = 1
                                    } else {
                                        rescaleState = 2
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                stringResource(
                                    if (mismatch.axis == DrawAxis.LENGTH) {
                                        R.string.draw_room_rescale_length
                                    } else {
                                        R.string.draw_room_rescale_width
                                    },
                                    formatMeters(mismatch.typedM)
                                )
                            )
                        }
                    }
                }
                if (mismatches.any { ImageRescale.factor(it.drawnM, it.typedM) != null }) {
                    Text(
                        text = stringResource(R.string.draw_room_rescale_note),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (rescaleState == 1) {
                    Text(
                        text = stringResource(R.string.draw_room_rescale_done),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (rescaleState == 2) {
                    Text(
                        text = stringResource(R.string.draw_room_rescale_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    saving = true
                    onSubmit(RoomInput(name, length, width, height, "")) { result ->
                        saving = false
                        when (result) {
                            is RoomSaveResult.Success -> onDone()
                            is RoomSaveResult.Invalid -> errors = result.errors
                            RoomSaveResult.NotFound -> onDone()
                        }
                    }
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun DrawRoomField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String?,
    hint: String?,
    decimal: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = errorText != null,
        supportingText = {
            if (errorText != null || hint != null) Text(errorText ?: hint.orEmpty())
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}
