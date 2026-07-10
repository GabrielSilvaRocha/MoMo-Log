package br.com.mo2log.mobile.ui

import android.content.Context
import android.graphics.Color
import kotlin.math.roundToInt

object Mo2Colors {
    val Background: Int = Color.rgb(15, 23, 42)
    val Surface: Int = Color.rgb(17, 24, 39)
    val SurfaceAlt: Int = Color.rgb(30, 41, 59)
    val SurfaceElevated: Int = Color.rgb(15, 36, 33)
    val Border: Int = Color.rgb(51, 65, 85)
    val Primary: Int = Color.rgb(34, 197, 94)
    val PrimarySoft: Int = Color.rgb(20, 83, 45)
    val TextPrimary: Int = Color.rgb(248, 250, 252)
    val TextSecondary: Int = Color.rgb(148, 163, 184)
    val TextMuted: Int = Color.rgb(100, 116, 139)
    val Warning: Int = Color.rgb(245, 158, 11)
    val Error: Int = Color.rgb(239, 68, 68)
    val Running: Int = Color.rgb(14, 165, 233)
}

object Mo2Spacing {
    const val Xs = 4
    const val Sm = 8
    const val Md = 12
    const val Lg = 16
    const val Xl = 20
    const val Xxl = 24
}

object Mo2Radius {
    const val Sm = 10
    const val Md = 14
    const val Lg = 18
    const val Pill = 999
}

object Mo2Type {
    const val Label = 12f
    const val Body = 14f
    const val BodyLarge = 16f
    const val Title = 22f
    const val Display = 30f
}

fun Context.mo2Dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
