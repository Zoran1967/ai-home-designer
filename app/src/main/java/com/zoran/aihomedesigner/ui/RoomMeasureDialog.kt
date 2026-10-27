package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.zoran.aihomedesigner.domain.MeasureKind
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.RoomInputErrors
import com.zoran.aihomedesigner.domain.RoomMeasureEdit

/**
 * Prozor za izmenu jedne mere sobe (dužina, širina ili visina zida) koja je dodirnuta na planu.
 * Menja se samo ta mera, a ime i ostale mere sobe ostaju kako jesu. Upisana mera je mera sobe.
 */
@Composable
fun RoomMeasureDialog(
    room: Room,
    kind: MeasureKind,
    onSubmit: (RoomInput, (RoomSaveResult) -> Unit) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    var text by rememberSaveable(room.id, kind) {
        mutableStateOf(RoomMeasureEdit.inputText(RoomMeasureEdit.valueOf(room, kind)))
    }
    var errors by remember { mutableStateOf<RoomInputErrors?>(null) }
    var saving by remember { mutableStateOf(false) }

    val (minM, maxM) = RoomMeasureEdit.limits(kind)
    val fieldError = errors?.let {
        when (kind) {
            MeasureKind.LENGTH -> it.length
            MeasureKind.WIDTH -> it.width
            MeasureKind.HEIGHT -> it.height
        }
    }
    val otherError = errors?.hasErrors == true && fieldError == null

    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = {
            Text(
                stringResource(
                    when (kind) {
                        MeasureKind.LENGTH -> R.string.measure_title_length
                        MeasureKind.WIDTH -> R.string.measure_title_width
                        MeasureKind.HEIGHT -> R.string.measure_title_height
                    }
                ) + ": " + room.name
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        errors = null
                    },
                    label = { Text(stringResource(R.string.measure_field)) },
                    singleLine = true,
                    isError = fieldError != null,
                    supportingText = {
                        Text(
                            if (fieldError != null) {
                                dimensionErrorText(fieldError, minM, maxM)
                            } else {
                                stringResource(R.string.measure_range, formatMeters(minM), formatMeters(maxM))
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                when (kind) {
                    MeasureKind.LENGTH -> Text(
                        text = stringResource(R.string.measure_grow_length),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    MeasureKind.WIDTH -> Text(
                        text = stringResource(R.string.measure_grow_width),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    MeasureKind.HEIGHT -> Unit
                }
                if (otherError) {
                    Text(
                        text = stringResource(R.string.measure_other_error),
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
                    onSubmit(RoomMeasureEdit.input(room, kind, text)) { result ->
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
