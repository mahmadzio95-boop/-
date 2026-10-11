package com.example.signage

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import java.io.File
import kotlin.concurrent.thread

/** Скрытое меню: открывается 5 нажатиями в центре экрана + PIN (по умолчанию 1234). */
class SettingsActivity : Activity() {
    private val names = listOf("Пусто", "Логотип", "Погода", "Часы")
    private val keys = listOf("none", "logo", "weather", "clock")
    private val corners = listOf("Левый верхний угол", "Правый верхний угол", "Левый нижний угол", "Правый нижний угол")
    private val spinners = ArrayList<Spinner>()
    private lateinit var preview: ImageView
    private lateinit var server: EditText
    private lateinit var city: EditText
    private lateinit var pin: EditText
    private lateinit var size: SeekBar
    private lateinit var mute: Switch
    private lateinit var blackout: Switch

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val c = this
        val col = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(c, 24), Ui.dp(c, 16), Ui.dp(c, 24), Ui.dp(c, 32))
        }
        fun add(v: View) = col.addView(v, LinearLayout.LayoutParams(-1, -2))

        add(TextView(c).apply { text = "Настройки"; textSize = 28f; setTextColor(Color.WHITE) })

        add(Ui.title(c, "Сервер"))
        server = Ui.edit(c, "http://адрес:порт", Prefs.server(c)); add(server)

        add(Ui.title(c, "Что показывать по углам"))
        for (i in 0..3) {
            add(Ui.label(c, corners[i]))
            val sp = Spinner(c)
            sp.adapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, names)
            sp.setSelection(keys.indexOf(Prefs.corner(c, i)).coerceAtLeast(0))
            spinners.add(sp); add(sp)
        }

        add(Ui.title(c, "Логотип"))
        preview = ImageView(c).apply { adjustViewBounds = true; maxHeight = Ui.dp(c, 90); scaleType = ImageView.ScaleType.FIT_START }
        add(preview); showPreview()
        add(Ui.button(c, "Выбрать логотип из галереи") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
            }, 1)
        })
        add(Ui.spacer(c, 6))
        add(Ui.button(c, "Удалить логотип", false) {
            Sync.logoFile(c).delete(); Prefs.put(c, "logo_src", ""); showPreview()
        })
        add(Ui.label(c, "Размер логотипа"))
        size = SeekBar(c).apply { max = 260; progress = Prefs.int(c, "logo_size", 120) - 40 }
        add(size)

        add(Ui.title(c, "Погода"))
        add(Ui.label(c, "Город"))
        city = Ui.edit(c, "Например: Душанбе", Prefs.str(c, "city")); add(city)

        add(Ui.title(c, "Управление"))
        mute = Switch(c).apply { text = "Выключить звук"; setTextColor(Color.WHITE); isChecked = Prefs.bool(c, "mute") }
        add(mute)
        blackout = Switch(c).apply { text = "Чёрный экран (вручную)"; setTextColor(Color.WHITE); isChecked = Prefs.bool(c, "blackout") }
        add(blackout)
        add(Ui.label(c, "PIN-код для входа в меню"))
        pin = Ui.edit(c, "1234", Prefs.pin(c), pass = true); add(pin)

        add(Ui.spacer(c, 20))
        add(Ui.button(c, "Синхронизировать сейчас", false) {
            Toast.makeText(c, "Синхронизация…", Toast.LENGTH_SHORT).show()
            thread { Sync.run(c); runOnUiThread { Toast.makeText(c, "Готово", Toast.LENGTH_SHORT).show() } }
        })
        add(Ui.spacer(c, 6))
        add(Ui.button(c, "Сохранить и вернуться") { save() })
        add(Ui.spacer(c, 6))
        add(Ui.button(c, "Выйти из приложения", false) { finishAffinity() })

        setContentView(ScrollView(c).apply { setBackgroundColor(Ui.BG); addView(col) })
    }

    private fun showPreview() {
        val f = Sync.logoFile(this)
        if (f.exists()) preview.setImageBitmap(decode(f)) else preview.setImageDrawable(null)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (req == 1 && res == RESULT_OK) {
            contentResolver.openInputStream(uri)?.use { i -> Sync.logoFile(this).outputStream().use { i.copyTo(it) } }
            Prefs.put(this, "logo_src", "local")   // чтобы серверный логотип не затёр выбранный вручную
            showPreview()
        }
    }

    private fun save() {
        Prefs.setServer(this, server.text.toString())
        spinners.forEachIndexed { i, s -> Prefs.setCorner(this, i, keys[s.selectedItemPosition]) }
        Prefs.putInt(this, "logo_size", size.progress + 40)
        Prefs.putBool(this, "mute", mute.isChecked)
        Prefs.putBool(this, "blackout", blackout.isChecked)
        pin.text.toString().takeIf { it.length >= 4 }?.let { Prefs.put(this, "pin", it) }
        val name = city.text.toString().trim()
        if (name.isNotEmpty() && name != Prefs.str(this, "city")) {
            thread {
                val p = Weather.geocode(name)
                runOnUiThread {
                    if (p == null) Toast.makeText(this, "Город не найден", Toast.LENGTH_LONG).show()
                    else {
                        Prefs.put(this, "city", p.name); Prefs.put(this, "lat", p.lat.toString()); Prefs.put(this, "lon", p.lon.toString())
                    }
                    finish()
                }
            }
        } else finish()
    }

    companion object {
        fun decode(f: File): android.graphics.Bitmap? {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, o)
            var s = 1
            while (o.outWidth / s > 1024) s *= 2
            return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
        }
    }
}
