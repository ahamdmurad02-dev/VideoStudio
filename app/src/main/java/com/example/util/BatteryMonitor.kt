package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryInfo(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val isCritical: Boolean = false
)

object BatteryMonitor {

    fun getCurrentBatteryInfo(context: Context): BatteryInfo {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            parseBatteryIntent(intent)
        } catch (_: Exception) {
            BatteryInfo(level = 100, isCharging = false, isCritical = false)
        }
    }

    fun observeBatteryInfo(context: Context): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    trySend(parseBatteryIntent(intent))
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        try {
            val stickyIntent = context.registerReceiver(receiver, filter)
            if (stickyIntent != null) {
                trySend(parseBatteryIntent(stickyIntent))
            }
        } catch (_: Exception) {
            trySend(BatteryInfo(level = 100, isCharging = false, isCritical = false))
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    private fun parseBatteryIntent(intent: Intent?): BatteryInfo {
        if (intent == null) return BatteryInfo()
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            100
        }

        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            isCritical = batteryPct <= 5
        )
    }
}
