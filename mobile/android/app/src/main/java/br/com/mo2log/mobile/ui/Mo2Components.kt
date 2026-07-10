package br.com.mo2log.mobile.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

object Mo2Components {
    fun label(
        context: Context,
        text: String,
        color: Int = Mo2Colors.TextPrimary,
        size: Float = Mo2Type.Body,
        bold: Boolean = false,
    ): TextView {
        val view = TextView(context)
        view.text = text
        view.setTextColor(color)
        view.textSize = size
        view.setLineSpacing(0f, 1.08f)
        if (bold) view.typeface = Typeface.DEFAULT_BOLD
        return view
    }

    fun kicker(context: Context, text: String): TextView {
        return label(
            context,
            text.uppercase(Locale("pt", "BR")),
            Mo2Colors.Primary,
            Mo2Type.Label,
            true,
        )
    }

    fun card(context: Context, color: Int = Mo2Colors.Surface): LinearLayout {
        val box = LinearLayout(context)
        box.setPadding(
            context.mo2Dp(Mo2Spacing.Lg),
            context.mo2Dp(Mo2Spacing.Lg),
            context.mo2Dp(Mo2Spacing.Lg),
            context.mo2Dp(Mo2Spacing.Lg),
        )
        box.background = Mo2Drawables.rounded(context, color, Mo2Radius.Lg, Mo2Colors.Border)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        params.setMargins(0, context.mo2Dp(Mo2Spacing.Sm), 0, context.mo2Dp(Mo2Spacing.Sm))
        box.layoutParams = params
        return box
    }

    fun badge(context: Context, text: String, active: Boolean = true): TextView {
        val view = label(
            context,
            text.uppercase(Locale("pt", "BR")),
            if (active) Mo2Colors.Background else Mo2Colors.TextPrimary,
            Mo2Type.Label,
            true,
        )
        view.gravity = Gravity.CENTER
        view.setPadding(context.mo2Dp(Mo2Spacing.Md), 0, context.mo2Dp(Mo2Spacing.Md), 0)
        view.background = Mo2Drawables.rounded(
            context,
            if (active) Mo2Colors.Primary else Mo2Colors.SurfaceAlt,
            Mo2Radius.Pill,
            if (active) Mo2Colors.Primary else Mo2Colors.Border,
        )
        return view
    }

    fun actionButton(
        context: Context,
        text: String,
        color: Int = Mo2Colors.Primary,
        textColor: Int = Mo2Colors.Background,
    ): Button {
        val button = Button(context)
        button.text = text
        button.textSize = Mo2Type.Body
        button.typeface = Typeface.DEFAULT_BOLD
        button.setTextColor(textColor)
        button.background = Mo2Drawables.pressed(
            context,
            color,
            Mo2Colors.SurfaceAlt,
            Mo2Radius.Md,
            if (color == Mo2Colors.Primary) Mo2Colors.Primary else Mo2Colors.Border,
        )
        button.isAllCaps = false
        button.minHeight = 0
        button.minimumHeight = 0
        button.includeFontPadding = false
        return button
    }

    fun progressBar(
        context: Context,
        percent: Int,
        fillColor: Int = Mo2Colors.Primary,
    ): LinearLayout {
        val clamped = percent.coerceIn(0, 100)
        val bar = LinearLayout(context)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.background = Mo2Drawables.rounded(context, Mo2Colors.SurfaceAlt, Mo2Radius.Pill, Mo2Colors.Border)

        val fill = View(context)
        fill.background = Mo2Drawables.rounded(context, fillColor, Mo2Radius.Pill)
        val empty = View(context)

        val fillWeight = if (clamped == 0) 0.0001f else clamped.toFloat()
        val emptyWeight = if (clamped == 100) 0.0001f else (100 - clamped).toFloat()
        bar.addView(fill, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, fillWeight))
        bar.addView(empty, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, emptyWeight))
        return bar
    }

    fun metricCard(
        context: Context,
        title: String,
        value: String,
        detail: String,
        accent: Int = Mo2Colors.Primary,
    ): LinearLayout {
        val box = card(context, Mo2Colors.SurfaceAlt)
        box.orientation = LinearLayout.VERTICAL
        box.addView(kicker(context, title))
        box.addView(label(context, value, Mo2Colors.TextPrimary, Mo2Type.Title, true))
        if (detail.isNotBlank()) box.addView(label(context, detail, Mo2Colors.TextSecondary, Mo2Type.Label, false))
        val marker = View(context)
        marker.background = Mo2Drawables.rounded(context, accent, Mo2Radius.Pill)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.mo2Dp(4))
        params.setMargins(0, context.mo2Dp(Mo2Spacing.Md), 0, 0)
        box.addView(marker, params)
        return box
    }

    fun timerDisplay(
        context: Context,
        value: String,
        caption: String,
        active: Boolean,
    ): LinearLayout {
        val box = card(context, if (active) Mo2Colors.SurfaceElevated else Mo2Colors.Surface)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.addView(label(context, value, if (active) Mo2Colors.Warning else Mo2Colors.TextPrimary, 44f, true))
        box.addView(label(context, caption, Mo2Colors.TextSecondary, Mo2Type.Label, false))
        return box
    }
}
