package com.example.signage

import android.content.Context
import java.io.File
import java.net.URL

object Sync {
    fun videoDir(c: Context) = File(c.filesDir, "videos").apply { mkdirs() }
    fun logoFile(c: Context) = File(c.filesDir, "logo.img")

    fun run(c: Context) {
        val base = Prefs.server(c)
        if (base.isEmpty()) return
        runCatching { Api.config(base) }.getOrNull()?.let { cfg ->
            Prefs.setSchedule(c, cfg.on, cfg.off)
            // Логотип с сервера (если задан) — скачиваем один раз для каждого нового URL
            if (cfg.logoUrl.isNotEmpty() && Prefs.str(c, "logo_src") != cfg.logoUrl) {
                runCatching {
                    URL(cfg.logoUrl).openStream().use { i -> logoFile(c).outputStream().use { i.copyTo(it) } }
                    Prefs.put(c, "logo_src", cfg.logoUrl)
                }
            }
        }
        val list = runCatching { Api.videos(base) }.getOrNull() ?: return
        val dir = videoDir(c)
        val keep = list.map { "${it.id}_${it.name}" }.toSet()
        dir.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
        for (v in list) {
            val f = File(dir, "${v.id}_${v.name}")
            if (f.exists()) continue
            val tmp = File(dir, f.name + ".part")
            runCatching {
                URL(v.url).openStream().use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                tmp.renameTo(f)
            }.onFailure { tmp.delete() }
        }
        runCatching { Api.ping(base) }
    }
}
