package com.example.signage

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("signage", Context.MODE_PRIVATE)
    fun str(c: Context, k: String, d: String = "") = sp(c).getString(k, d) ?: d
    fun put(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()
    fun int(c: Context, k: String, d: Int) = sp(c).getInt(k, d)
    fun putInt(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()
    fun bool(c: Context, k: String, d: Boolean = false) = sp(c).getBoolean(k, d)
    fun putBool(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()

    fun server(c: Context) = str(c, "server")
    fun setServer(c: Context, v: String) = put(c, "server", v.trim())
    fun onTime(c: Context) = str(c, "on", "08:00")
    fun offTime(c: Context) = str(c, "off", "22:00")
    fun setSchedule(c: Context, on: String, off: String) { put(c, "on", on); put(c, "off", off) }

    /** Что показывать в углу: none | logo | weather | clock. Углы: 0=лев.верх 1=прав.верх 2=лев.низ 3=прав.низ */
    fun corner(c: Context, i: Int) = str(c, "corner$i", listOf("logo", "weather", "none", "clock")[i])
    fun setCorner(c: Context, i: Int, v: String) = put(c, "corner$i", v)
    fun pin(c: Context) = str(c, "pin", "1234")
}
