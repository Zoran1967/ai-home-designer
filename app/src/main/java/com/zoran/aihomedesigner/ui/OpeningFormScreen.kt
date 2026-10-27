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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.zoran.aihomedesigner.data.OpeningSaveResult
import com.zoran.aihomedesigner.domain.DoorSwing
import com.zoran.aihomedesigner.domain.HingeEnd
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningInput
import com.zoran.aihomedesigner.domain.OpeningInputErrors
import com.zoran.aihomedesigner.domain.OpeningLimits
import com.zoran.aihomedesigner.domain.OpeningPlacementError
import com.zoran.aihomedesigner.domain.OpeningValidator
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomPlacement
import com.zoran.aihomedesigner.domain.WallGeometry
import com.zoran.aihomedesigner.domain.WallSide
import kotlin.math.max

/** Ekran za dodavanje (initialOpening = null) ili izmenu vrata i prozora. */
@Composable
fun OpeningFormScreen(
    room: Room,
    initialOpening: Opening?,
    onSubmit: (OpeningInput, (OpeningSaveResult) -> Unit) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    initialSide: WallSide? = null,
    tapCenterM: Double? = null
) {
    val key = initialOpening?.id
    var type by rememberSaveable(key) { mutableStateOf(initialOpening?.type ?: OpeningType.DOOR) }
    var side by rememberSaveable(key) { mutableStateOf(initialOpening?.side ?: initialSide ?: WallSide.NORTH) }
    var offset by rememberSaveable(key) {
        mutableStateOf(initialOpening?.let { metersToInputText(it.offsetM) } ?: "")
    }
    var width by rememberSaveable(key) {
        mutableStateOf(initialOpening?.let { metersToInputText(it.widthM) } ?: "")
    }
    var height by rememberSaveable(key) {
        mutableStateOf(initialOpening?.let { metersToInputText(it.heightM) } ?: "")
    }
    var sill by rememberSaveable(key) {
        mutableStateOf(
            if (initialOpening != null && initialOpening.type == OpeningType.WINDOW) {
                metersToInputText(initialOpening.sillHeightM)
            } else {
                ""
            }
        )
    }
    var swing by rememberSaveable(key) { mutableStateOf(initialOpening?.swing) }
    var hinge by rememberSaveable(key) { mutableStateOf(initialOpening?.hinge) }
    var errors by remember { mutableStateOf<OpeningInputErrors?>(null) }
    var saving by remember { mutableStateOf(false) }
    // Dok korisnik ne promeni položaj ili zid ručno, položaj se računa iz mesta dodira na planu
    var offsetTouched by rememberSaveable(key) { mutableStateOf(false) }

    val isDoor = type == OpeningType.DOOR
    val wallLength = WallGeometry.wallLengthM(room, side)

    val tapOffsetText: String? = if (initialOpening == null && tapCenterM != null && !offsetTouched) {
        val w = OpeningValidator.parseMeters(width)
        val start = if (w == null) tapCenterM else (tapCenterM - w / 2).coerceIn(0.0, max(0.0, wallLength - w))
        metersToInputText(RoomPlacement.roundToMm(start))
    } else {
        null
    }
    val shownOffset = tapOffsetText ?: offset

    val widthMin = if (isDoor) OpeningLimits.DOOR_MIN_WIDTH_M else OpeningLimits.WINDOW_MIN_WIDTH_M
    val widthMax = if (isDoor) OpeningLimits.DOOR_MAX_WIDTH_M else OpeningLimits.WINDOW_MAX_WIDTH_M
    val heightMin = if (isDoor) OpeningLimits.DOOR_MIN_HEIGHT_M else OpeningLimits.WINDOW_MIN_HEIGHT_M
    val heightMax = if (isDoor) OpeningLimits.DOOR_MAX_HEIGHT_M else OpeningLimits.WINDOW_MAX_HEIGHT_M

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
                    if (initialOpening == null) R.string.create_opening_title else R.string.edit_opening_title
                ),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = room.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            if (initialOpening == null && tapCenterM != null) {
                Text(
                    text = stringResource(R.string.opening_from_tap_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            SectionTitle(stringResource(R.string.label_opening_type))
            OpeningType.values().forEach { option ->
                ChoiceRow(
                    selected = type == option,
                    label = openingTypeLabel(option),
                    onClick = {
                        type = option
                        errors = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.label_opening_wall))
            WallSide.values().forEach { option ->
                ChoiceRow(
                    selected = side == option,
                    label = wallSideLabel(option),
                    onClick = {
                        offset = shownOffset
                        offsetTouched = true
                        side = option
                        errors = errors?.copy(placement = null)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            MeasureField(
                label = stringResource(R.string.field_opening_offset),
                value = shownOffset,
                onValueChange = {
                    offset = it
                    offsetTouched = true
                    errors = errors?.copy(offset = null, placement = null)
                },
                errorText = errors?.offset?.let {
                    dimensionErrorText(it, 0.0, OpeningLimits.OFFSET_MAX_M)
                } ?: errors?.placement?.let {
                    placementErrorText(it, wallLength, room.heightM, errors?.maxOffsetM)
                },
                hint = stringResource(
                    R.string.hint_opening_offset,
                    wallStartLabel(side),
                    formatMeters(wallLength)
                )
            )
            val centeredOffset = OpeningValidator.parseMeters(width)
                ?.let { OpeningValidator.centeredOffsetM(wallLength, it) }
            TextButton(
                enabled = centeredOffset != null,
                onClick = {
                    if (centeredOffset != null) {
                        offset = metersToInputText(centeredOffset)
                        offsetTouched = true
                        errors = errors?.copy(offset = null, placement = null)
                    }
                }
            ) {
                Text(stringResource(R.string.opening_center_button))
            }
            Spacer(modifier = Modifier.height(8.dp))

            MeasureField(
                label = stringResource(R.string.field_opening_width),
                value = width,
                onValueChange = {
                    width = it
                    errors = errors?.copy(width = null, placement = null)
                },
                errorText = errors?.width?.let { dimensionErrorText(it, widthMin, widthMax) },
                hint = stringResource(R.string.hint_opening_width)
            )
            Spacer(modifier = Modifier.height(8.dp))

            MeasureField(
                label = stringResource(R.string.field_opening_height),
                value = height,
                onValueChange = {
                    height = it
                    errors = errors?.copy(height = null, placement = null)
                },
                errorText = errors?.height?.let { dimensionErrorText(it, heightMin, heightMax) },
                hint = stringResource(R.string.hint_opening_height)
            )

            if (!isDoor) {
                Spacer(modifier = Modifier.height(8.dp))
                MeasureField(
                    label = stringResource(R.string.field_opening_sill),
                    value = sill,
                    onValueChange = {
                        sill = it
                        errors = errors?.copy(sillHeight = null, placement = null)
                    },
                    errorText = errors?.sillHeight?.let {
                        dimensionErrorText(it, 0.0, OpeningLimits.SILL_MAX_M)
                    },
                    hint = stringResource(R.string.hint_opening_sill)
                )
            }

            if (isDoor) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.label_opening_swing))
                DoorSwing.values().forEach { option ->
                    ChoiceRow(
                        selected = swing == option,
                        label = doorSwingLabel(option),
                        onClick = {
                            swing = option
                            errors = errors?.copy(swingMissing = false, hingeMissing = false)
                        }
                    )
                }
                if (errors?.swingMissing == true) {
                    ErrorLine(stringResource(R.string.error_opening_swing_missing))
                }

                if (swing != null && swing != DoorSwing.SLIDING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.label_opening_hinge))
                    HingeEnd.values().forEach { option ->
                        ChoiceRow(
                            selected = hinge == option,
                            label = hingeLabel(side, option),
                            onClick = {
                                hinge = option
                                errors = errors?.copy(hingeMissing = false)
                            }
                        )
                    }
                    if (errors?.hingeMissing == true) {
                        ErrorLine(stringResource(R.string.error_opening_hinge_missing))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        val input = OpeningInput(
                            type = type,
                            side = side,
                            offset = shownOffset,
                            width = width,
                            height = height,
                            sillHeight = sill,
                            swing = if (isDoor) swing else null,
                            hinge = if (isDoor) hinge else null
                        )
                        onSubmit(input) { result ->
                            saving = false
                            when (result) {
                                is OpeningSaveResult.Success -> onDone()
                                is OpeningSaveResult.Invalid -> errors = result.errors
                                OpeningSaveResult.NotFound -> onDone()
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
private fun placementErrorText(
    error: OpeningPlacementError,
    wallLengthM: Double,
    roomHeightM: Double,
    maxOffsetM: Double?
): String = when (error) {
    OpeningPlacementError.EXCEEDS_WALL ->
        if (maxOffsetM != null && maxOffsetM >= 0.0) {
            stringResource(
                R.string.error_opening_exceeds_wall,
                formatMeters(wallLengthM),
                formatMeters(maxOffsetM)
            )
        } else {
            stringResource(R.string.error_opening_wider_than_wall, formatMeters(wallLengthM))
        }

    OpeningPlacementError.TOO_TALL_FOR_ROOM ->
        stringResource(R.string.error_opening_too_tall, formatMeters(roomHeightM))

    OpeningPlacementError.OVERLAPS_OTHER ->
        stringResource(R.string.error_opening_overlaps)
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ErrorLine(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
    )
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
private fun MeasureField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String?,
    hint: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = errorText != null,
        supportingText = { Text(errorText ?: hint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
