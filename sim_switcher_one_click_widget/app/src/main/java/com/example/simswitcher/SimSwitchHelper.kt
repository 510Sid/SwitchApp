package com.example.simswitcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SimStatus(
    val activeSlot: Int,
    val activeCarrier: String,
    val inactiveSlot: Int,
    val inactiveCarrier: String,
    val inactiveSubId: Int,
    val signalBars: Int,
    val downSpeedMbps: Int
)

object SimSwitchHelper {

    private const val PREFS_NAME = "sim_prefs"
    private const val KEY_HISTORY = "switch_history_log"
    private const val KEY_SIM1_NICK = "sim1_nickname"
    private const val KEY_SIM2_NICK = "sim2_nickname"

    fun setSimNicknames(context: Context, sim1Name: String, sim2Name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SIM1_NICK, sim1Name.trim())
            .putString(KEY_SIM2_NICK, sim2Name.trim())
            .apply()
        SimSwitchWidget.updateAllWidgets(context)
    }

    fun getCustomNickname(context: Context, slot: Int, defaultName: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (slot == 1) KEY_SIM1_NICK else KEY_SIM2_NICK
        val saved = prefs.getString(key, "")?.trim().orEmpty()
        return if (saved.isNotEmpty()) saved else defaultName
    }

    @SuppressLint("MissingPermission")
    fun getSimStatus(context: Context): SimStatus {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val signalBars = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            telephony.signalStrength?.level ?: 4
        } else 4

        val activeNet = connManager.activeNetwork
        val caps = connManager.getNetworkCapabilities(activeNet)
        val downKbps = caps?.linkDownstreamBandwidthKbps ?: 45000
        val downMbps = (downKbps / 1000).coerceAtLeast(1)

        val activeDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()

        try {
            val subs: List<SubscriptionInfo>? = subManager.activeSubscriptionInfoList
            if (subs != null && subs.size >= 2) {
                val activeSub = subs.find { it.subscriptionId == activeDataSubId } ?: subs[0]
                val inactiveSub = subs.first { it.subscriptionId != activeSub.subscriptionId }
                val activeSlot = activeSub.simSlotIndex + 1
                val inactiveSlot = inactiveSub.simSlotIndex + 1
                return SimStatus(
                    activeSlot = activeSlot,
                    activeCarrier = getCustomNickname(context, activeSlot, activeSub.displayName.toString()),
                    inactiveSlot = inactiveSlot,
                    inactiveCarrier = getCustomNickname(context, inactiveSlot, inactiveSub.displayName.toString()),
                    inactiveSubId = inactiveSub.subscriptionId,
                    signalBars = signalBars,
                    downSpeedMbps = downMbps
                )
            }
        } catch (_: SecurityException) {
            // Fallback if READ_PHONE_STATE is not granted yet
        }

        val currentSlot = prefs.getInt("saved_active_slot", 1)
        val nextSlot = if (currentSlot == 1) 2 else 1
        return SimStatus(
            activeSlot = currentSlot,
            activeCarrier = getCustomNickname(context, currentSlot, "SIM $currentSlot"),
            inactiveSlot = nextSlot,
            inactiveCarrier = getCustomNickname(context, nextSlot, "SIM $nextSlot"),
            inactiveSubId = nextSlot,
            signalBars = signalBars,
            downSpeedMbps = downMbps
        )
    }

    // Manual 1-Click switch to the currently INACTIVE SIM + Haptic Pulse + History Log
    fun switchToInactiveSim(context: Context, source: String = "App"): SimStatus {
        val current = getSimStatus(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            Settings.Global.putInt(
                context.contentResolver,
                "multi_sim_data_call",
                current.inactiveSubId
            )
            prefs.edit().putInt("saved_active_slot", current.inactiveSlot).apply()
            triggerHapticPulse(context)
            recordSwitchHistory(context, current.inactiveSlot, current.inactiveCarrier, source)
            Toast.makeText(
                context,
                "Switched Mobile Data to SIM ${current.inactiveSlot} (${current.inactiveCarrier})",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: SecurityException) {
            val panelIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(panelIntent)
        }

        SimSwitchWidget.updateAllWidgets(context)
        return getSimStatus(context)
    }

    @Suppress("DEPRECATION")
    private fun triggerHapticPulse(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(40L)
        }
    }

    private fun recordSwitchHistory(context: Context, targetSlot: Int, carrier: String, source: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "$timeStr • SIM $targetSlot ($carrier) via $source"
        val existing = prefs.getString(KEY_HISTORY, "") ?: ""
        val updated = (listOf(entry) + existing.split("\n").filter { it.isNotBlank() }).take(5).joinToString("\n")
        prefs.edit().putString(KEY_HISTORY, updated).apply()
    }

    fun getSwitchHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        return raw.split("\n").filter { it.isNotBlank() }
    }

    fun clearSwitchHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
