package com.example.signage

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Video(val id: String, val name: String, val url: String)
data class Config(val on: String, val off: String, val logoUrl: String = "")

/**
 * ПРЕДПОЛОЖЕНИЕ о формате сервера (поправьте под свой бэкенд):
 *  GET  /apis/config    -> {"switchOnTime":"08:00","switchOffTime":"22:00","logoUrl":"http://.../logo.png"}  (logoUrl необязателен)
 *  GET  /apis/videolist -> [{"id":"1","name":"a.mp4","url":"http://.../a.mp4"}, ...]
 *  POST /apis/ping      -> любой 200 OK
 */
object Api {
    private fun open(base: String, path: String): HttpURLConnection {
        val c = URL(base.trimEnd('/') + path).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 20_000
        return c
    }

    fun config(base: String): Config {
        val j = JSONObject(open(base, "/apis/config").inputStream.bufferedReader().use { it.readText() })
        return Config(j.optString("switchOnTime", "08:00"), j.optString("switchOffTime", "22:00"),
            j.optString("logoUrl", ""))
    }

    fun videos(base: String): List<Video> {
        val a = JSONArray(open(base, "/apis/videolist").inputStream.bufferedReader().use { it.readText() })
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Video(o.getString("id"), o.getString("name"), o.getString("url"))
        }
    }

    fun ping(base: String) {
        val c = open(base, "/apis/ping")
        c.requestMethod = "POST"
        c.doOutput = true
        c.outputStream.use { it.write("{}".toByteArray()) }
        c.responseCode
    }
}
