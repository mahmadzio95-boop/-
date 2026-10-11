package com.example.signage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Автозапуск после перезагрузки. На Android 10+ может блокироваться системой —
 *  для киоска лучше сделать приложение лаунчером или Device Owner. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) {
            c.startActivity(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
