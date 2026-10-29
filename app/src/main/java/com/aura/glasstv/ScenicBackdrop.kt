package com.aura.glasstv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.min
import kotlin.random.Random

/** Original procedurally painted scenic wallpapers. No copied Apple artwork. */
class ScenicBackdrop(context: Context) : View(context) {
    var scene: Int = 0
        set(value) { field = value; invalidate() }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stars = List(36) {
        // Seeded so the star field does not flicker on redraw.
        val r = Random(it * 107 + 42)
        Triple(r.nextFloat(), r.nextFloat() * 0.72f, r.nextFloat() * 1.8f + 0.5f)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val palette = when (scene) {
            1 -> intArrayOf(0xFF041522.toInt(), 0xFF264B5A.toInt(), 0xFF57778B.toInt(), 0xFF0D2233.toInt())
            2 -> intArrayOf(0xFF231D35.toInt(), 0xFF65516D.toInt(), 0xFFC38285.toInt(), 0xFF2F2E4A.toInt())
            else -> intArrayOf(0xFF08182F.toInt(), 0xFF243B62.toInt(), 0xFF7990AB.toInt(), 0xFF12233B.toInt())
        }
        p.shader = LinearGradient(0f, 0f, w * 0.15f, h, palette, floatArrayOf(0f, .35f, .72f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, p)
        p.shader = null

        // Light bloom (static shader for cheap, predictable rendering).
        val cx = w * if (scene == 2) .72f else .77f
        val cy = h * if (scene == 2) .37f else .42f
        val radius = min(w, h) * .60f
        val bloom = if (scene == 2) 0x5CFFE1C8 else 0x4FC9EDFF
        p.shader = RadialGradient(cx, cy, radius, intArrayOf(bloom, 0x16C6DDFD, Color.TRANSPARENT),
            floatArrayOf(0f, .35f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, p)
        p.shader = null

        for ((x, y, size) in stars) {
            p.color = 0x88FFFFFF.toInt()
            canvas.drawCircle(x * w, y * h, size * resources.displayMetrics.density * .65f, p)
        }
        // Wide far ridge, kept low to leave the hero title unobstructed.
        val distant = Path().apply {
            moveTo(0f, h * .81f)
            cubicTo(w * .15f, h * .69f, w * .29f, h * .75f, w * .45f, h * .71f)
            cubicTo(w * .64f, h * .67f, w * .73f, h * .76f, w, h * .64f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        p.shader = LinearGradient(0f, h * .62f, 0f, h, 0x703E5875, 0xE0091B33.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(distant, p)
        p.shader = null

        val foreground = Path().apply {
            moveTo(0f, h * .88f)
            cubicTo(w * .20f, h * .84f, w * .27f, h * .91f, w * .42f, h * .87f)
            cubicTo(w * .68f, h * .82f, w * .87f, h * .90f, w, h * .80f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        p.color = 0xA9081327.toInt()
        canvas.drawPath(foreground, p)
        p.color = Color.WHITE
    }
}
