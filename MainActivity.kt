package com.example.signage

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import kotlin.concurrent.thread

/** Первичная настройка. Дальше приложение сразу открывает плеер. */
class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Prefs.server(this).isNotEmpty()) { go(); return }

        val url = Ui.edit(this, "http://192.168.0.10:8080", "")
        val status = TextView(this).apply { setTextColor(0xFF9AA3B2.toInt()); setPadding(0, Ui.dp(context, 12), 0, 0) }
        val btn = Ui.button(this, "Сохранить и запустить") {}
        btn.setOnClickListener {
            Prefs.setServer(this, url.text.toString())
            btn.isEnabled = false
            status.text = "Загрузка видео…"
            thread { Sync.run(this); runOnUiThread { go() } }
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.round(Ui.CARD, Ui.dp(context, 20).toFloat())
            setPadding(Ui.dp(context, 28), Ui.dp(context, 24), Ui.dp(context, 28), Ui.dp(context, 24))
            addView(TextView(context).apply { text = "Signage Player"; textSize = 26f; setTextColor(Color.WHITE) })
            addView(Ui.label(context, "Адрес сервера"))
            addView(url, LinearLayout.LayoutParams(-1, -2))
            addView(Ui.spacer(context, 14))
            addView(btn, LinearLayout.LayoutParams(-1, -2))
            addView(status)
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Ui.BG)
            addView(card, FrameLayout.LayoutParams(Ui.dp(context, 460), -2, Gravity.CENTER))
        })
    }

    private fun go() {
        SyncWorker.schedule(this)
        startActivity(Intent(this, PlayerActivity::class.java))
        finish()
    }
}
