package com.kalp.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoveWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        for (id in ids) {
            updateWidget(context, manager, id)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            id: Int
        ) {
            val views = RemoteViews(
                context.packageName,
                R.layout.love_widget
            )

            val prefs = context.getSharedPreferences(
                "kalp_data",
                Context.MODE_PRIVATE
            )

            val start = prefs.getString("relationship_start", null)

            if (start != null) {
                try {
                    val date = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ).parse(start.take(10))!!

                    val elapsed = (System.currentTimeMillis() - date.time)
                        .coerceAtLeast(0L)

                    val days = elapsed / 86400000L
                    val hours = elapsed / 3600000L

                    views.setTextViewText(
                        R.id.widget_time,
                        "$days gün  •  $hours saat"
                    )
                } catch (_: Exception) {
                    views.setTextViewText(
                        R.id.widget_time,
                        "Tarih bekleniyor"
                    )
                }
            } else {
                views.setTextViewText(
                    R.id.widget_time,
                    "Tarih seçilmedi"
                )
            }

            val distance = prefs.getString("distance", null)

            views.setTextViewText(
                R.id.widget_distance,
                if (distance != null) "♥ $distance km" else "♥ Konum bekleniyor"
            )

            manager.updateAppWidget(id, views)
        }
    }
}
