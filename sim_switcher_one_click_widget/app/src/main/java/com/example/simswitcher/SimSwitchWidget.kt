package com.example.simswitcher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SimSwitchWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidgetUI(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ONE_CLICK_SWITCH) {
            SimSwitchHelper.switchToInactiveSim(context, source = "Widget")
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_ONE_CLICK_SWITCH = "com.example.simswitcher.ACTION_ONE_CLICK_SWITCH"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SimSwitchWidget::class.java))
            for (id in ids) {
                updateWidgetUI(context, manager, id)
            }
        }

        private fun updateWidgetUI(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val status = SimSwitchHelper.getSimStatus(context)
            val views = RemoteViews(context.packageName, R.layout.sim_widget)

            val signalHint = if (status.signalBars <= 1) "⚠️ Weak (${status.signalBars}/4)" else "${status.signalBars}/4 bars • ${status.downSpeedMbps} Mbps"
            views.setTextViewText(
                R.id.widgetActiveText,
                "Active: SIM ${status.activeSlot} (${status.activeCarrier}) • $signalHint"
            )
            views.setTextViewText(
                R.id.widgetSwitchBtn,
                "Switch to SIM ${status.inactiveSlot} (${status.inactiveCarrier})"
            )

            val clickIntent = Intent(context, SimSwitchWidget::class.java).apply {
                action = ACTION_ONE_CLICK_SWITCH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetSwitchBtn, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
