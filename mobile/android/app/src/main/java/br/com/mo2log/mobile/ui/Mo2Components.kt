package br.com.mo2log.mobile.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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

    fun card(context: Context, color: Int = Mo2Colors.SurfaceAlt): LinearLayout {
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
        view.includeFontPadding = false
        view.minHeight = context.mo2Dp(28)
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
        button.textSize = Mo2Type.Button
        button.typeface = Typeface.DEFAULT_BOLD
        button.setTextColor(
            ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(Mo2Colors.TextMuted, textColor),
            ),
        )
        button.background = Mo2Drawables.pressed(
            context = context,
            color = color,
            pressedColor = if (color == Mo2Colors.Primary) Mo2Colors.PrimaryDark else Mo2Colors.SurfaceElevated,
            disabledColor = Mo2Colors.Disabled,
            radiusDp = Mo2Radius.Md,
            strokeColor = if (color == Mo2Colors.Primary) Mo2Colors.Primary else Mo2Colors.Border,
        )
        button.foreground = Mo2Drawables.rippleForeground(
            context,
            Color.argb(52, 248, 250, 252),
            Mo2Radius.Md,
        )
        button.isAllCaps = false
        button.gravity = Gravity.CENTER
        button.contentDescription = text
        button.minHeight = context.mo2Dp(48)
        button.minimumHeight = context.mo2Dp(48)
        button.minWidth = context.mo2Dp(48)
        button.minimumWidth = context.mo2Dp(48)
        button.includeFontPadding = false
        button.setPadding(context.mo2Dp(Mo2Spacing.Lg), 0, context.mo2Dp(Mo2Spacing.Lg), 0)
        button.elevation = 0f
        return button
    }

    fun bottomNavigationItem(
        context: Context,
        title: String,
        icon: Mo2NavIcon,
        active: Boolean,
        onClick: () -> Unit,
    ): LinearLayout {
        val color = if (active) Mo2Colors.Primary else Mo2Colors.TextSecondary
        val item = LinearLayout(context)
        item.orientation = LinearLayout.VERTICAL
        item.gravity = Gravity.CENTER
        item.setPadding(
            context.mo2Dp(Mo2Spacing.Xs),
            context.mo2Dp(Mo2Spacing.Xs),
            context.mo2Dp(Mo2Spacing.Xs),
            context.mo2Dp(Mo2Spacing.Xs),
        )
        item.background = Mo2Drawables.rounded(
            context,
            if (active) Mo2Colors.SurfaceAlt else Color.TRANSPARENT,
            Mo2Radius.Md,
        )
        item.foreground = Mo2Drawables.rippleForeground(
            context,
            Color.argb(44, 34, 197, 94),
            Mo2Radius.Md,
        )
        item.minimumHeight = context.mo2Dp(64)
        item.isClickable = true
        item.isFocusable = true
        item.isSelected = active
        item.contentDescription = if (active) "$title, selecionado" else title
        item.setOnClickListener { onClick() }

        val indicator = View(context)
        indicator.background = Mo2Drawables.rounded(
            context,
            if (active) Mo2Colors.Primary else Color.TRANSPARENT,
            Mo2Radius.Pill,
        )
        val indicatorParams = LinearLayout.LayoutParams(context.mo2Dp(24), context.mo2Dp(3))
        indicatorParams.setMargins(0, 0, 0, context.mo2Dp(Mo2Spacing.Xs))
        item.addView(indicator, indicatorParams)

        val iconView = Mo2NavIconView(context, icon, color)
        item.addView(iconView, LinearLayout.LayoutParams(context.mo2Dp(24), context.mo2Dp(24)))

        val label = label(context, title, color, Mo2Type.Label, active)
        label.gravity = Gravity.CENTER
        label.includeFontPadding = false
        val labelParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        labelParams.setMargins(0, context.mo2Dp(Mo2Spacing.Xs), 0, 0)
        item.addView(label, labelParams)
        return item
    }

    fun sectionHeader(context: Context, text: String): TextView {
        val view = label(context, text, Mo2Colors.TextPrimary, Mo2Type.SectionTitle, true)
        view.setPadding(0, context.mo2Dp(Mo2Spacing.Xxl), 0, context.mo2Dp(Mo2Spacing.Sm))
        return view
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
        box.addView(label(context, value, Mo2Colors.TextPrimary, Mo2Type.SectionTitle, true))
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
        val box = card(context, if (active) Mo2Colors.SurfaceElevated else Mo2Colors.SurfaceAlt)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.addView(label(context, value, if (active) Mo2Colors.Warning else Mo2Colors.TextPrimary, Mo2Type.Timer, true))
        box.addView(label(context, caption, Mo2Colors.TextSecondary, Mo2Type.Label, false))
        return box
    }
}
