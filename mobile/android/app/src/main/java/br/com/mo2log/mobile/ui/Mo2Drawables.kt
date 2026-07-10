package br.com.mo2log.mobile.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable

object Mo2Drawables {
    fun rounded(
        context: Context,
        color: Int,
        radiusDp: Int = Mo2Radius.Md,
        strokeColor: Int? = null,
        strokeDp: Int = 1,
    ): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = context.mo2Dp(radiusDp).toFloat()
        if (strokeColor != null) drawable.setStroke(context.mo2Dp(strokeDp), strokeColor)
        return drawable
    }

    fun pressed(
        context: Context,
        color: Int,
        pressedColor: Int,
        radiusDp: Int = Mo2Radius.Md,
        strokeColor: Int? = null,
    ): StateListDrawable {
        val states = StateListDrawable()
        states.addState(
            intArrayOf(android.R.attr.state_pressed),
            rounded(context, pressedColor, radiusDp, strokeColor),
        )
        states.addState(
            intArrayOf(),
            rounded(context, color, radiusDp, strokeColor),
        )
        return states
    }

    fun roundedPx(
        context: Context,
        color: Int,
        radiusPx: Int,
        strokeColor: Int? = null,
        strokeDp: Int = 1,
    ): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = radiusPx.toFloat()
        if (strokeColor != null) drawable.setStroke(context.mo2Dp(strokeDp), strokeColor)
        return drawable
    }
}
