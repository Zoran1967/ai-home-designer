package com.zoran.aihomedesigner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.domain.DoorSwing
import com.zoran.aihomedesigner.domain.HingeEnd
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.WallSide

@Composable
fun openingTypeLabel(type: OpeningType): String = when (type) {
    OpeningType.DOOR -> stringResource(R.string.opening_door)
    OpeningType.WINDOW -> stringResource(R.string.opening_window)
}

@Composable
fun wallSideLabel(side: WallSide): String = when (side) {
    WallSide.NORTH -> stringResource(R.string.side_north)
    WallSide.EAST -> stringResource(R.string.side_east)
    WallSide.SOUTH -> stringResource(R.string.side_south)
    WallSide.WEST -> stringResource(R.string.side_west)
}

/** Od kog ugla sobe se računa položaj otvora na tom zidu. */
@Composable
fun wallStartLabel(side: WallSide): String = when (side) {
    WallSide.NORTH, WallSide.SOUTH -> stringResource(R.string.start_west)
    WallSide.EAST, WallSide.WEST -> stringResource(R.string.start_north)
}

@Composable
fun doorSwingLabel(swing: DoorSwing): String = when (swing) {
    DoorSwing.INTO_ROOM -> stringResource(R.string.swing_into_room)
    DoorSwing.OUT_OF_ROOM -> stringResource(R.string.swing_out_of_room)
    DoorSwing.SLIDING -> stringResource(R.string.swing_sliding)
}

@Composable
fun hingeLabel(side: WallSide, end: HingeEnd): String {
    val horizontalWall = side == WallSide.NORTH || side == WallSide.SOUTH
    return when {
        horizontalWall && end == HingeEnd.START -> stringResource(R.string.hinge_left)
        horizontalWall -> stringResource(R.string.hinge_right)
        end == HingeEnd.START -> stringResource(R.string.hinge_top)
        else -> stringResource(R.string.hinge_bottom)
    }
}
