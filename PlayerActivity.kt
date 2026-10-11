package com.example.signage

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var view: PlayerView
    private lateinit var root: FrameLayout
    private val h = Handler(Looper.getMainLooper())
    private var knownFiles = ""
    private val overlays = ArrayList<View>()
    private val weatherViews = ArrayList<TextView>()
    private val clockViews = ArrayList<TextView>()
    private var lastWeather = 0L
    private val taps = ArrayList<Long>()

    private val tick = object : Runnable {
        override fun run() {
            refresh(); updateClock(); maybeWeather()
            h.postDelayed(this, 10_000)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view = PlayerView(this).apply { useController = false }
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK); addView(view) }
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        player = ExoPlayer.Builder(this).build().also {
            it.repeatMode = Player.REPEAT_MODE_ALL
            view.player = it
        }
        knownFiles = ""
    }

    override fun onResume() {
        super.onResume()
        hideBars()
        buildOverlays()
        lastWeather = 0
        h.removeCallbacks(tick); tick.run()
    }

    override fun onPause() { h.removeCallbacks(tick); super.onPause() }

    override fun onStop() {
        player?.release(); player = null
        super.onStop()
    }

    @Deprecated("kiosk")
    override fun onBackPressed() { /* в режиме киоска назад не работает */ }

    // ---------- Скрытое меню: 5 нажатий в центре экрана за 4 секунды ----------
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val w = root.width; val hh = root.height
            if (ev.x in w * 0.3f..w * 0.7f && ev.y in hh * 0.3f..hh * 0.7f) {
                val now = SystemClock.uptimeMillis()
                taps.add(now); taps.removeAll { now - it > 4000 }
                if (taps.size >= 5) { taps.clear(); askPin() }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun askPin() {
        val et = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN"
        }
        AlertDialog.Builder(this).setTitle("Введите PIN").setView(et)
            .setPositiveButton("OK") { _, _ ->
                if (et.text.toString() == Prefs.pin(this)) startActivity(android.content.Intent(this, SettingsActivity::class.java))
                else Toast.makeText(this, "Неверный PIN", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Отмена", null).show()
    }

    // ---------- Оверлеи по углам ----------
    private fun buildOverlays() {
        overlays.forEach { root.removeView(it) }
        overlays.clear(); weatherViews.clear(); clockViews.clear()
        val gravs = intArrayOf(Gravity.TOP or Gravity.START, Gravity.TOP or Gravity.END,
            Gravity.BOTTOM or Gravity.START, Gravity.BOTTOM or Gravity.END)
        val m = Ui.dp(this, 20)
        for (i in 0..3) {
            val v: View = when (Prefs.corner(this, i)) {
                "logo" -> logoView() ?: continue
                "weather" -> pill().also { it.text = Prefs.str(this, "wtext", "…"); weatherViews.add(it) }
                "clock" -> pill().also { clockViews.add(it) }
                else -> continue
            }
            root.addView(v, FrameLayout.LayoutParams(-2, -2, gravs[i]).apply { setMargins(m, m, m, m) })
            overlays.add(v)
        }
        updateClock()
    }

    private fun pill() = TextView(this).apply {
        setTextColor(Color.WHITE); textSize = 26f
        background = Ui.round(0x99000000.toInt(), Ui.dp(context, 16).toFloat())
        setPadding(Ui.dp(context, 18), Ui.dp(context, 10), Ui.dp(context, 18), Ui.dp(context, 10))
    }

    private fun logoView(): View? {
        val f = Sync.logoFile(this)
        val bmp = if (f.exists()) SettingsActivity.decode(f) else null
        bmp ?: return null
        val size = Ui.dp(this, Prefs.int(this, "logo_size", 120))
        return ImageView(this).apply {
            setImageBitmap(bmp); adjustViewBounds = true
            maxWidth = size; maxHeight = size; scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    private fun updateClock() {
        if (clockViews.isEmpty()) return
        val t = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        clockViews.forEach { it.text = t }
    }

    private fun maybeWeather() {
        if (weatherViews.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastWeather < 15 * 60_000) return
        lastWeather = now
        val lat = Prefs.str(this, "lat").toDoubleOrNull() ?: return
        val lon = Prefs.str(this, "lon").toDoubleOrNull() ?: return
        thread {
            val n = Weather.fetch(lat, lon)
            if (n == null) { lastWeather = now - 14 * 60_000; return@thread }   // повтор через минуту
            val t = "${Weather.icon(n.code)} ${Math.round(n.temp)}°C  ${Prefs.str(this, "city")}"
            Prefs.put(this, "wtext", t)
            runOnUiThread { weatherViews.forEach { it.text = t } }
        }
    }

    // ---------- Воспроизведение ----------
    private fun refresh() {
        val p = player ?: return
        if (!inSchedule()) { p.pause(); view.visibility = View.INVISIBLE; return }
        view.visibility = View.VISIBLE
        p.volume = if (Prefs.bool(this, "mute")) 0f else 1f
        val files = Sync.videoDir(this).listFiles { f -> !f.name.endsWith(".part") }
            ?.sortedBy { it.name } ?: emptyList()
        val sig = files.joinToString("|") { it.name }
        if (sig != knownFiles) {
            knownFiles = sig
            p.setMediaItems(files.map { MediaItem.fromUri(android.net.Uri.fromFile(it)) })
            p.prepare()
        }
        if (files.isNotEmpty()) p.play()
    }

    private fun inSchedule(): Boolean {
        if (Prefs.bool(this, "blackout")) return false
        fun min(s: String) = s.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val on = runCatching { min(Prefs.onTime(this)) }.getOrDefault(0)
        val off = runCatching { min(Prefs.offTime(this)) }.getOrDefault(1440)
        return if (on <= off) now in on until off else now >= on || now < off
    }

    @Suppress("DEPRECATION")
    private fun hideBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}
