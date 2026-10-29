package com.kalp.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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

            val time = SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(Date())

            val prefs = context.getSharedPreferences(
                "kalp_data",
                Context.MODE_PRIVATE
            )

            val code = prefs.getString("pair_code", null)

            views.setTextViewText(R.id.widget_time, time)
            views.setTextViewText(
                R.id.widget_distance,
                if (code == null) "♥ Eşleşmeni bekliyor" else "♥ Kalpler bağlı"
            )

            manager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == Intent.ACTION_TIME_TICK ||
            intent.action == Intent.ACTION_TIME_CHANGED) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, LoveWidget::class.java)
            )
            onUpdate(context, manager, ids)
        }
    }
}
