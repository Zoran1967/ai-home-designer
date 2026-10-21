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
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureCategory
import com.zoran.aihomedesigner.domain.FurnitureIssue
import com.zoran.aihomedesigner.domain.FurniturePassages
import com.zoran.aihomedesigner.domain.Room

@Composable
fun furnitureCategoryLabel(category: FurnitureCategory): String = when (category) {
    FurnitureCategory.BED -> stringResource(R.string.furniture_cat_bed)
    FurnitureCategory.SOFA -> stringResource(R.string.furniture_cat_sofa)
    FurnitureCategory.TABLE -> stringResource(R.string.furniture_cat_table)
    FurnitureCategory.CHAIR -> stringResource(R.string.furniture_cat_chair)
    FurnitureCategory.WARDROBE -> stringResource(R.string.furniture_cat_wardrobe)
    FurnitureCategory.DESK -> stringResource(R.string.furniture_cat_desk)
    FurnitureCategory.SHELF -> stringResource(R.string.furniture_cat_shelf)
    FurnitureCategory.APPLIANCE -> stringResource(R.string.furniture_cat_appliance)
    FurnitureCategory.OTHER -> stringResource(R.string.furniture_cat_other)
}

@Composable
fun FurnitureListScreen(
    room: Room,
    furniture: List<Furniture>?,
    issues: List<FurnitureIssue>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Furniture) -> Unit,
    onDelete: (Furniture) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<Furniture?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd) {
                Text(stringResource(R.string.furniture_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(
                text = stringResource(R.string.furniture_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(text = room.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.furniture_room_info,
                    formatMeters(room.lengthM),
                    formatMeters(room.widthM),
                    formatMeters(room.heightM)
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when {
                furniture == null -> Unit // još se učitava
                furniture.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.furniture_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    val gaps = FurniturePassages.narrowGaps(furniture)
                    issues.forEach { issue ->
                        Text(
                            text = issueText(issue, furniture),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    gaps.forEach { gap ->
                        val nameA = furniture.firstOrNull { it.id == gap.furnitureAId }?.name ?: "?"
                        val nameB = furniture.firstOrNull { it.id == gap.furnitureBId }?.name ?: "?"
                        Text(
                            text = stringResource(
                                R.string.furniture_gap_note,
                                nameA,
                                nameB,
                                formatCentimeters(gap.gapM)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(
                        text = stringResource(
                            if (issues.isEmpty()) R.string.furniture_checked_ok else R.string.furniture_checked
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.furniture_tap_to_edit),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(furniture, key = { it.id }) { item ->
                            FurnitureCard(
                                item = item,
                                onOpen = { onOpen(item) },
                                onDelete = { itemToDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.delete_furniture_title)) },
            text = { Text(stringResource(R.string.delete_furniture_message, item.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item)
                    itemToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun issueText(issue: FurnitureIssue, furniture: List<Furniture>): String {
    fun name(id: String): String = furniture.firstOrNull { it.id == id }?.name ?: "?"
    return when (issue) {
        is FurnitureIssue.OutsideRoom ->
            stringResource(R.string.furniture_issue_outside, name(issue.furnitureId))

        is FurnitureIssue.TooTall ->
            stringResource(R.string.furniture_issue_too_tall, name(issue.furnitureId))

        is FurnitureIssue.Overlap -> stringResource(
            R.string.furniture_issue_overlap,
            name(issue.furnitureAId),
            name(issue.furnitureBId)
        )

        is FurnitureIssue.BlocksDoor ->
            stringResource(R.string.furniture_issue_blocks_door, name(issue.furnitureId))
    }
}

@Composable
private fun FurnitureCard(item: Furniture, onOpen: () -> Unit, onDelete: () -> Unit) {
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
                        R.string.furniture_title_line,
                        item.name,
                        furnitureCategoryLabel(item.category)
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        R.string.furniture_size_line,
                        formatMeters(item.lengthM),
                        formatMeters(item.widthM),
                        formatMeters(item.heightM)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.furniture_rotation_line, item.rotationDeg),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dimensionSourceLabel(item.dimensionSource),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
        }
    }
}
