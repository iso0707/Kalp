package com.kalp.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import java.util.concurrent.TimeUnit

class LoveWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("kalp", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", true)
        val start = prefs.getLong("start_date", 0L)
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.love_widget)
            if (!enabled) {
                views.setTextViewText(R.id.distance, "♥")
                views.setTextViewText(R.id.time, "Sayaç kapalı")
            } else {
                views.setTextViewText(R.id.distance, "Konumlar bekleniyor")
                if (start <= 0L) views.setTextViewText(R.id.time, "Birliktelik tarihini ayarla")
                else {
                    val days = TimeUnit.MILLISECONDS.toDays((System.currentTimeMillis() - start).coerceAtLeast(0))
                    views.setTextViewText(R.id.time, "$days gündür birliktesiniz")
                }
            }
            manager.updateAppWidget(id, views)
        }
    }
}
