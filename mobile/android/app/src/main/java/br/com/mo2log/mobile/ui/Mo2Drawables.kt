package br.com.mo2log.mobile.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
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
        disabledColor: Int = Mo2Colors.Disabled,
        radiusDp: Int = Mo2Radius.Md,
        strokeColor: Int? = null,
    ): StateListDrawable {
        val states = StateListDrawable()
        states.addState(
            intArrayOf(-android.R.attr.state_enabled),
            rounded(context, disabledColor, radiusDp, Mo2Colors.Border),
        )
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

    fun rippleForeground(
        context: Context,
        color: Int,
        radiusDp: Int = Mo2Radius.Md,
    ): RippleDrawable {
        val mask = rounded(context, android.graphics.Color.WHITE, radiusDp)
        return RippleDrawable(ColorStateList.valueOf(color), null, mask)
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
