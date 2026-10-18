package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.zoran.aihomedesigner.domain.Room

@Composable
fun RoomListScreen(
    projectName: String,
    rooms: List<Room>?,
    onBack: () -> Unit,
    onAddRoom: () -> Unit,
    onOpenRoom: (Room) -> Unit,
    onOpenPlan: () -> Unit,
    onDeleteRoom: (Room) -> Unit
) {
    var roomToDelete by remember { mutableStateOf<Room?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddRoom) {
                Text(stringResource(R.string.new_room))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!rooms.isNullOrEmpty()) {
                    TextButton(onClick = onOpenPlan) {
                        Text(stringResource(R.string.plan_button))
                    }
                }
            }
            Text(
                text = projectName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when {
                rooms == null -> Unit // još se učitava
                rooms.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.rooms_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.rooms_tap_to_edit),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(rooms, key = { it.id }) { room ->
                            RoomCard(
                                room = room,
                                onOpen = { onOpenRoom(room) },
                                onDelete = { roomToDelete = room }
                            )
                        }
                    }
                }
            }
        }
    }

    roomToDelete?.let { room ->
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            title = { Text(stringResource(R.string.delete_room_title)) },
            text = { Text(stringResource(R.string.delete_room_message, room.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRoom(room)
                    roomToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun RoomCard(room: Room, onOpen: () -> Unit, onDelete: () -> Unit) {
    val thickness = room.wallThicknessM
    val thicknessText = if (thickness != null) {
        stringResource(R.string.room_thickness, formatMeters(thickness))
    } else {
        stringResource(R.string.room_thickness_none)
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        R.string.room_dimensions,
                        formatMeters(room.lengthM),
                        formatMeters(room.widthM),
                        formatArea(room.areaM2)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.room_height, formatMeters(room.heightM)) +
                        " · " + thicknessText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dimensionSourceLabel(room.dimensionSource),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}
