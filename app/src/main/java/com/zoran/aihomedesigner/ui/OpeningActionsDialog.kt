package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.data.OpeningSaveResult
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningActions
import com.zoran.aihomedesigner.domain.OpeningInput
import com.zoran.aihomedesigner.domain.Room

/**
 * Brze radnje nad vratima ili prozorom dodirnutim na planu: okretanje otvaranja, okretanje šarki,
 * izmena svih mera i brisanje (uz potvrdu).
 */
@Composable
fun OpeningActionsDialog(
    opening: Opening,
    room: Room,
    onApply: (OpeningInput, (OpeningSaveResult) -> Unit) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.opening_actions_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.opening_actions_delete_message,
                        openingTypeLabel(opening.type),
                        wallSideLabel(opening.side),
                        room.name
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
        return
    }

    fun apply(input: OpeningInput?) {
        if (input == null) return
        failed = false
        onApply(input) { result -> if (result !is OpeningSaveResult.Success) failed = true }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(stringResource(R.string.opening_actions_title, openingTypeLabel(opening.type), room.name))
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.opening_actions_info,
                        wallSideLabel(opening.side),
                        formatMeters(opening.widthM),
                        formatMeters(opening.heightM)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                opening.swing?.let { swing ->
                    Text(
                        text = stringResource(R.string.opening_actions_swing_now, doorSwingLabel(swing)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                opening.hinge?.let { hinge ->
                    Text(
                        text = stringResource(R.string.opening_actions_hinge_now, hingeLabel(opening.side, hinge)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (OpeningActions.canFlip(opening)) {
                    TextButton(onClick = { apply(OpeningActions.flipSwing(opening)) }) {
                        Text(stringResource(R.string.opening_actions_flip_swing))
                    }
                    TextButton(onClick = { apply(OpeningActions.flipHinge(opening)) }) {
                        Text(stringResource(R.string.opening_actions_flip_hinge))
                    }
                }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.opening_actions_edit)) }
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete)) }
                if (failed) {
                    Text(
                        text = stringResource(R.string.opening_actions_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.opening_actions_close)) }
        }
    )
}
