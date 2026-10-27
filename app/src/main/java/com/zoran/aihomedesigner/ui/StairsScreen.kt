package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zoran.aihomedesigner.data.StairsSaveResult
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.Stairs
import com.zoran.aihomedesigner.domain.StairsAdvice
import com.zoran.aihomedesigner.domain.StairsGeometry
import com.zoran.aihomedesigner.domain.StairsInput
import com.zoran.aihomedesigner.domain.StairsIssue
import com.zoran.aihomedesigner.domain.StairsLayoutValidator
import com.zoran.aihomedesigner.domain.StairsInputErrors
import com.zoran.aihomedesigner.domain.StairsLimits
import com.zoran.aihomedesigner.domain.StairsPlanning

/** Spisak stepenica projekta, sa procenom broja stepenika i savetima o udobnosti. */
@Composable
fun StairsListScreen(
    projectName: String,
    stairs: List<Stairs>?,
    rooms: List<Room>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Stairs) -> Unit,
    onDelete: (Stairs) -> Unit
) {
    var toDelete by remember { mutableStateOf<Stairs?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd) {
                Text(stringResource(R.string.stairs_new))
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
                text = stringResource(R.string.stairs_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(text = projectName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.stairs_not_drawn_yet),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(R.string.stairs_checks_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (stairs != null && stairs.isEmpty()) {
                Text(
                    text = stringResource(R.string.stairs_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stairs.orEmpty(), key = { it.id }) { item ->
                    StairsCard(
                        item = item,
                        riseM = StairsPlanning.riseM(rooms, item.lowerLevel),
                        issues = StairsLayoutValidator.validate(item, stairs.orEmpty(), rooms),
                        onEdit = { onEdit(item) },
                        onDelete = { toDelete = item }
                    )
                }
            }
        }
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.stairs_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.stairs_delete_message,
                        floorLabel(item.lowerLevel),
                        floorLabel(item.upperLevel)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item)
                    toDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun StairsCard(
    item: Stairs,
    riseM: Double,
    issues: List<StairsIssue>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val comfort = StairsGeometry.analyze(riseM, item.lengthM)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.stairs_card_title,
                    floorLabel(item.lowerLevel),
                    floorLabel(item.upperLevel)
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.stairs_card_size,
                    formatMeters(item.lengthM),
                    formatMeters(item.widthM),
                    stairsTowardLabel(item.directionDeg)
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(
                    R.string.stairs_card_steps,
                    comfort.stepCount,
                    formatCentimeters(comfort.riserM),
                    formatCentimeters(comfort.treadM)
                )
            )
            comfort.advice.forEach { advice ->
                Text(
                    text = when (advice) {
                        StairsAdvice.RISER_TOO_HIGH -> stringResource(
                            R.string.stairs_advice_riser,
                            formatCentimeters(StairsGeometry.MAX_COMFORT_RISER_M)
                        )
                        StairsAdvice.TREAD_TOO_SHORT -> stringResource(
                            R.string.stairs_advice_tread,
                            formatCentimeters(StairsGeometry.MIN_COMFORT_TREAD_M)
                        )
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            issues.forEach { issue ->
                Text(
                    text = when (issue) {
                        StairsIssue.NotInsideRoomBelow ->
                            stringResource(R.string.stairs_issue_below, floorLabel(item.lowerLevel))
                        StairsIssue.NotInsideRoomAbove ->
                            stringResource(R.string.stairs_issue_above, floorLabel(item.upperLevel))
                        is StairsIssue.OverlapsStairs -> stringResource(R.string.stairs_issue_overlap)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = stringResource(R.string.stairs_card_rise_note, formatMeters(riseM)),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.stairs_edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

/** Ekran za dodavanje (initialStairs = null) ili izmenu stepenica. */
@Composable
fun StairsFormScreen(
    initialStairs: Stairs?,
    levels: List<Int>,
    defaultLevel: Int,
    onSubmit: (StairsInput, (StairsSaveResult) -> Unit) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val key = initialStairs?.id
    val options = (levels + listOfNotNull(initialStairs?.lowerLevel)).distinct().sorted()
    var level by rememberSaveable(key) {
        mutableIntStateOf(
            initialStairs?.lowerLevel
                ?: defaultLevel.takeIf { it in options }
                ?: options.firstOrNull()
                ?: 0
        )
    }
    var length by rememberSaveable(key) {
        mutableStateOf(initialStairs?.let { metersToInputText(it.lengthM) } ?: "")
    }
    var width by rememberSaveable(key) {
        mutableStateOf(initialStairs?.let { metersToInputText(it.widthM) } ?: "0,9")
    }
    var direction by rememberSaveable(key) { mutableIntStateOf(initialStairs?.directionDeg ?: 0) }
    var errors by remember { mutableStateOf<StairsInputErrors?>(null) }
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
                    if (initialStairs == null) R.string.stairs_create_title else R.string.stairs_edit_title
                ),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.stairs_label_level),
                style = MaterialTheme.typography.titleMedium
            )
            options.forEach { option ->
                ChoiceRow(
                    selected = level == option,
                    label = stringResource(
                        R.string.stairs_level_option,
                        floorLabel(option),
                        floorLabel(option + 1)
                    ),
                    onClick = {
                        level = option
                        errors = errors?.copy(levelInvalid = false)
                    }
                )
            }
            if (errors?.levelInvalid == true) {
                Text(
                    text = stringResource(R.string.stairs_error_level),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            StairsField(
                label = stringResource(R.string.stairs_field_length),
                value = length,
                onValueChange = {
                    length = it
                    errors = errors?.copy(length = null)
                },
                errorText = errors?.length?.let {
                    dimensionErrorText(it, StairsLimits.MIN_LENGTH_M, StairsLimits.MAX_LENGTH_M)
                },
                hint = stringResource(R.string.stairs_hint_length)
            )
            Spacer(modifier = Modifier.height(8.dp))
            StairsField(
                label = stringResource(R.string.stairs_field_width),
                value = width,
                onValueChange = {
                    width = it
                    errors = errors?.copy(width = null)
                },
                errorText = errors?.width?.let {
                    dimensionErrorText(it, StairsLimits.MIN_WIDTH_M, StairsLimits.MAX_WIDTH_M)
                },
                hint = stringResource(R.string.stairs_hint_width)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.stairs_label_direction),
                style = MaterialTheme.typography.titleMedium
            )
            listOf(0, 90, 180, 270).forEach { option ->
                ChoiceRow(
                    selected = direction == option,
                    label = stairsDirectionLabel(option),
                    onClick = { direction = option }
                )
            }
            Text(
                text = stringResource(R.string.stairs_direction_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        onSubmit(StairsInput(level, length, width, direction)) { result ->
                            saving = false
                            when (result) {
                                is StairsSaveResult.Success -> onDone()
                                is StairsSaveResult.Invalid -> errors = result.errors
                                StairsSaveResult.NotFound -> onDone()
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
private fun stairsDirectionLabel(degrees: Int): String = stringResource(
    when (degrees) {
        90 -> R.string.stairs_direction_90
        180 -> R.string.stairs_direction_180
        270 -> R.string.stairs_direction_270
        else -> R.string.stairs_direction_0
    }
)

@Composable
private fun stairsTowardLabel(degrees: Int): String = stringResource(
    when (degrees) {
        90 -> R.string.stairs_toward_90
        180 -> R.string.stairs_toward_180
        270 -> R.string.stairs_toward_270
        else -> R.string.stairs_toward_0
    }
)

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
private fun StairsField(
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
