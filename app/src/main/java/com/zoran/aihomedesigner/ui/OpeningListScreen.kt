package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.domain.DoorSwing
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.Room

@Composable
fun OpeningListScreen(
    room: Room,
    openings: List<Opening>?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Opening) -> Unit,
    onDelete: (Opening) -> Unit
) {
    var openingToDelete by remember { mutableStateOf<Opening?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd) {
                Text(stringResource(R.string.opening_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.openings_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = room.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(
                    R.string.openings_walls_info,
                    formatMeters(room.lengthM),
                    formatMeters(room.widthM)
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when {
                openings == null -> Unit // još se učitava
                openings.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.openings_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.openings_tap_to_edit),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(openings, key = { it.id }) { opening ->
                            OpeningCard(
                                opening = opening,
                                onOpen = { onOpen(opening) },
                                onDelete = { openingToDelete = opening }
                            )
                        }
                    }
                }
            }
        }
    }

    openingToDelete?.let { opening ->
        AlertDialog(
            onDismissRequest = { openingToDelete = null },
            title = { Text(stringResource(R.string.delete_opening_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_opening_message,
                        openingTypeLabel(opening.type),
                        wallSideLabel(opening.side)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(opening)
                    openingToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { openingToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun OpeningCard(opening: Opening, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.opening_title_line,
                        openingTypeLabel(opening.type),
                        wallSideLabel(opening.side)
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        R.string.opening_size_line,
                        formatMeters(opening.widthM),
                        formatMeters(opening.heightM)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(
                        R.string.opening_position_line,
                        formatMeters(opening.offsetM),
                        wallStartLabel(opening.side)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (opening.type == OpeningType.WINDOW) {
                    Text(
                        text = stringResource(R.string.opening_sill_line, formatMeters(opening.sillHeightM)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    opening.swing?.let { swing ->
                        val hinge = opening.hinge
                        val text = if (swing != DoorSwing.SLIDING && hinge != null) {
                            doorSwingLabel(swing) + " · " + hingeLabel(opening.side, hinge)
                        } else {
                            doorSwingLabel(swing)
                        }
                        Text(text = text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    text = dimensionSourceLabel(opening.dimensionSource),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}
