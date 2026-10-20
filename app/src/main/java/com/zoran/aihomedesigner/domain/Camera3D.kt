package com.zoran.aihomedesigner.domain

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Kamera koja kruži oko tačke [target]. yawDeg 0 znači pogled sa juga ka severu (sever je na planu gore),
 * pitchDeg je visina kamere iznad horizonta (0 = u visini cilja, 90 = pogled odozgo).
 */
data class Camera3D(
    val yawDeg: Double,
    val pitchDeg: Double,
    val distanceM: Double,
    val target: Vec3,
    val fovDeg: Double = 50.0
)

/** Tačka na ekranu u pikselima, i njena dubina (rastojanje od kamere duž pravca gledanja). */
data class ScreenPoint(val x: Double, val y: Double, val depth: Double)

object Projection {

    const val NEAR_M = 0.05

    private fun normalize(v: Vec3): Vec3 {
        val length = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
        return Vec3(v.x / length, v.y / length, v.z / length)
    }

    private fun cross(a: Vec3, b: Vec3) = Vec3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x
    )

    private fun dot(a: Vec3, b: Vec3) = a.x * b.x + a.y * b.y + a.z * b.z

    fun eye(camera: Camera3D): Vec3 {
        val yaw = Math.toRadians(camera.yawDeg)
        val pitch = Math.toRadians(camera.pitchDeg)
        return Vec3(
            camera.target.x + camera.distanceM * sin(yaw) * cos(pitch),
            camera.target.y + camera.distanceM * cos(yaw) * cos(pitch),
            camera.target.z + camera.distanceM * sin(pitch)
        )
    }

    /**
     * Perspektivna projekcija tačke na ekran veličine [viewWidth] x [viewHeight] piksela.
     * Vraća null za tačke iza kamere ili preblizu njoj.
     */
    fun project(point: Vec3, camera: Camera3D, viewWidth: Double, viewHeight: Double): ScreenPoint? {
        val eye = eye(camera)
        val forward = normalize(Vec3(camera.target.x - eye.x, camera.target.y - eye.y, camera.target.z - eye.z))
        val up = Vec3(0.0, 0.0, 1.0)
        // Kad kamera gleda tačno odozgo, "gore" na ekranu je sever (-y na planu)
        val reference = if (kotlin.math.abs(forward.z) > 0.9999) Vec3(0.0, -1.0, 0.0) else up
        val right = normalize(cross(reference, forward))
        val cameraUp = cross(forward, right)

        val v = Vec3(point.x - eye.x, point.y - eye.y, point.z - eye.z)
        val depth = dot(v, forward)
        if (depth < NEAR_M) return null

        val focal = (viewHeight / 2.0) / tan(Math.toRadians(camera.fovDeg) / 2.0)
        val screenX = viewWidth / 2.0 + dot(v, right) / depth * focal
        val screenY = viewHeight / 2.0 - dot(v, cameraUp) / depth * focal
        return ScreenPoint(screenX, screenY, depth)
    }
}
