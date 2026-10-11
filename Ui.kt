package com.example.signage

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.View
import android.widget.*

object Ui {
    const val BG = 0xFF0F1115.toInt()
    const val CARD = 0xFF1B1F27.toInt()
    const val ACCENT = 0xFF4C8DFF.toInt()

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun round(color: Int, r: Float) = GradientDrawable().apply { setColor(color); cornerRadius = r }

    fun title(c: Context, t: String) = TextView(c).apply {
        text = t; textSize = 13f; setTextColor(ACCENT); isAllCaps = true
        setPadding(0, dp(c, 22), 0, dp(c, 6))
    }

    fun label(c: Context, t: String) = TextView(c).apply {
        text = t; textSize = 15f; setTextColor(Color.WHITE); setPadding(0, dp(c, 8), 0, dp(c, 2))
    }

    fun edit(c: Context, hint: String, value: String, pass: Boolean = false) = EditText(c).apply {
        setHint(hint); setText(value); setTextColor(Color.WHITE); setHintTextColor(0xFF7A808C.toInt())
        background = round(CARD, dp(c, 10).toFloat()); setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12))
        inputType = if (pass) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else InputType.TYPE_TEXT_VARIATION_URI
    }

    fun button(c: Context, t: String, primary: Boolean = true, onClick: () -> Unit) = Button(c).apply {
        text = t; setTextColor(Color.WHITE); isAllCaps = false
        background = round(if (primary) ACCENT else CARD, dp(c, 10).toFloat())
        setOnClickListener { onClick() }
    }

    fun spacer(c: Context, h: Int = 8) = View(c).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(c, h))
    }
}
