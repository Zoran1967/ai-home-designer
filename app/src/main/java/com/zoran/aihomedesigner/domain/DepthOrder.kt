package com.zoran.aihomedesigner.domain

/**
 * Redosled crtanja kvadara u 3D prikazu: dalji se crta pre bližeg, pa bliži prekriva dalji.
 *
 * Kvadri su poravnati sa osama. Ako dva kvadra razdvaja ravan (jedan je potpuno s jedne strane
 * drugog po nekoj osi) i kamera je s jedne strane te ravni, kvadar sa strane kamere je bliži
 * i mora da se crta kasnije. Sortiranje po rastojanju do središta to ne garantuje, naročito
 * kad se velika ploča (pod, dugi zid) poredi sa malim kvadrom (nameštaj).
 *
 * Kad kamera ne odlučuje (kvadri se preklapaju, kao vrata i zid u koji su ugrađena), vrata i prozori
 * idu posle zida. Ako se pravila međusobno poništavaju (ciklus), bira se najdalji kvadar.
 * Ovo je poboljšanje redosleda, ne potpuno rešenje skrivanja površina.
 */
object DepthOrder {

    /** Vraća iste kvadre, od najdaljih ka najbližim kameri na položaju [eye]. */
    fun farToNear(boxes: List<SceneBox>, eye: Vec3): List<SceneBox> {
        val n = boxes.size
        if (n < 2) return boxes

        val after = Array(n) { mutableListOf<Int>() }
        val waiting = IntArray(n)
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                when (order(boxes[i], boxes[j], eye)) {
                    1 -> { after[i].add(j); waiting[j]++ }
                    -1 -> { after[j].add(i); waiting[i]++ }
                    else -> Unit
                }
            }
        }

        val distance = DoubleArray(n) { distanceSquared(boxes[it], eye) }
        val done = BooleanArray(n)
        val result = ArrayList<SceneBox>(n)
        repeat(n) {
            var best = -1
            for (k in 0 until n) {
                if (done[k] || waiting[k] != 0) continue
                if (best == -1 || distance[k] > distance[best]) best = k
            }
            if (best == -1) { // ciklus: uzmi najdalji od preostalih
                for (k in 0 until n) {
                    if (done[k]) continue
                    if (best == -1 || distance[k] > distance[best]) best = k
                }
            }
            done[best] = true
            result += boxes[best]
            for (m in after[best]) waiting[m]--
        }
        return result
    }

    /** 1 ako [a] mora pre [b], -1 ako [a] mora posle [b], 0 ako redosled nije određen. */
    private fun order(a: SceneBox, b: SceneBox, eye: Vec3): Int {
        for (axis in 0..2) {
            val aMin = coord(a.min, axis)
            val aMax = coord(a.max, axis)
            val bMin = coord(b.min, axis)
            val bMax = coord(b.max, axis)
            val e = coord(eye, axis)
            if (aMax <= bMin) { // a je s niže strane b
                if (e >= bMin) return 1 // kamera sa strane b: b je bliži
                if (e <= aMax) return -1 // kamera sa strane a: a je bliži
            } else if (bMax <= aMin) {
                if (e >= aMin) return -1
                if (e <= bMax) return 1
            }
        }
        val aOpening = a.kind == SceneKind.DOOR || a.kind == SceneKind.WINDOW
        val bOpening = b.kind == SceneKind.DOOR || b.kind == SceneKind.WINDOW
        if (aOpening && b.kind == SceneKind.WALL) return -1
        if (a.kind == SceneKind.WALL && bOpening) return 1
        return 0
    }

    private fun coord(v: Vec3, axis: Int): Double = when (axis) {
        0 -> v.x
        1 -> v.y
        else -> v.z
    }

    private fun distanceSquared(box: SceneBox, eye: Vec3): Double {
        val dx = (box.min.x + box.max.x) / 2 - eye.x
        val dy = (box.min.y + box.max.y) / 2 - eye.y
        val dz = (box.min.z + box.max.z) / 2 - eye.z
        return dx * dx + dy * dy + dz * dz
    }
}
