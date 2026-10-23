package com.zoran.aihomedesigner.domain

/** Ishod pokušaja da se soba prebaci na drugi sprat. */
sealed interface FloorMoveResult {
    /** Soba je prebačena. [room] je nova verzija sobe (novi sprat i novi položaj na planu). */
    data class Moved(val room: Room) : FloorMoveResult

    /** Traženi sprat nije dozvoljen (van granica [Floors.MIN_LEVEL] do [Floors.MAX_LEVEL]). */
    data object InvalidFloor : FloorMoveResult

    /** Soba je već na tom spratu. */
    data object SameFloor : FloorMoveResult

    /** Na ciljnom spratu već postoji druga soba sa istim imenom. */
    data object NameTaken : FloorMoveResult

    /** Soba više ne postoji (npr. obrisana je). */
    data object NotFound : FloorMoveResult
}

/** Čista logika prebacivanja sobe na drugi sprat (bez baze i Androida). */
object FloorMove {

    /**
     * Proverava da li se [room] sme prebaciti na [targetLevel] i računa novu verziju sobe.
     * [projectRooms] su sve sobe istog projekta (sa svih spratova).
     *
     * Mere sobe se ne diraju. Položaj na planu je raspored, a ne izmerena vrednost, pa soba
     * dobija novi početni položaj desno od soba ciljnog sprata (stari položaj bi mogao da se
     * preklapa sa tamošnjim sobama). Vrata, prozori i nameštaj su vezani za sobu i idu sa njom.
     */
    fun move(room: Room, targetLevel: Int, projectRooms: List<Room>): FloorMoveResult {
        if (!Floors.isValidLevel(targetLevel)) return FloorMoveResult.InvalidFloor
        if (targetLevel == room.floorLevel) return FloorMoveResult.SameFloor

        val onTarget = projectRooms.filter { it.id != room.id && it.floorLevel == targetLevel }
        val name = ProjectValidator.normalizeName(room.name)
        val taken = onTarget.any { ProjectValidator.normalizeName(it.name).equals(name, ignoreCase = true) }
        if (taken) return FloorMoveResult.NameTaken

        val position = RoomPlacement.nextPosition(onTarget)
        return FloorMoveResult.Moved(
            room.copy(floorLevel = targetLevel, xM = position.xM, yM = position.yM)
        )
    }
}
