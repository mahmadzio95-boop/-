package com.example.signage

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Open-Meteo: бесплатно, без API-ключа. */
object Weather {
    data class Now(val temp: Double, val code: Int, val wind: Double)
    data class Place(val name: String, val lat: Double, val lon: Double)

    private fun get(u: String): String {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 10_000
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    fun fetch(lat: Double, lon: Double): Now? = runCatching {
        val j = JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,wind_speed_10m")).getJSONObject("current")
        Now(j.getDouble("temperature_2m"), j.getInt("weather_code"), j.getDouble("wind_speed_10m"))
    }.getOrNull()

    fun geocode(city: String): Place? = runCatching {
        val j = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?count=1&language=ru&name=" +
            URLEncoder.encode(city, "UTF-8")))
        val r = j.getJSONArray("results").getJSONObject(0)
        Place(r.getString("name"), r.getDouble("latitude"), r.getDouble("longitude"))
    }.getOrNull()

    fun icon(code: Int) = when (code) {
        0 -> "☀️"; 1, 2 -> "🌤️"; 3 -> "☁️"; 45, 48 -> "🌫️"
        in 51..57 -> "🌦️"; in 61..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"; in 95..99 -> "⛈️"; else -> "🌡️"
    }
}
