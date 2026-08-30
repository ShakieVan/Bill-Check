package de.shakie.billcheck

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.domain.CurrencyAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BillCheckWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { update(context, manager, it) }
    }

    companion object {
        const val ACTION_OPEN = "de.shakie.billcheck.widget.OPEN"
        const val ACTION_PHOTO = "de.shakie.billcheck.widget.PHOTO"
        const val ACTION_IMAGE = "de.shakie.billcheck.widget.IMAGE"
        const val ACTION_MANUAL = "de.shakie.billcheck.widget.MANUAL"
        const val ACTION_STATEMENT = "de.shakie.billcheck.widget.STATEMENT"
        const val SELECTED_TRIP_PREFERENCES = "widget_preferences"
        const val SELECTED_TRIP_ID = "selected_trip_id"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BillCheckWidget::class.java))
            ids.forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            scope.launch {
                val application = context.applicationContext as BillCheckApplication
                val dao = application.database.dao()
                val trips = dao.getAllTrips()
                val selectedId = context.getSharedPreferences(
                    SELECTED_TRIP_PREFERENCES,
                    Context.MODE_PRIVATE,
                ).getString(SELECTED_TRIP_ID, null)
                val trip = trips.firstOrNull { it.id == selectedId } ?: trips.firstOrNull()
                val receipts = trip?.let { dao.getReceiptsWithItems(it.id) }.orEmpty()
                val entities = receipts.map { it.receipt }
                val views = RemoteViews(context.packageName, R.layout.widget_bill_check).apply {
                    setTextViewText(R.id.widget_trip_name, trip?.name ?: context.getString(R.string.widget_no_trip))
                    setTextViewText(
                        R.id.widget_rounded_total,
                        context.getString(
                            R.string.widget_rounded_total,
                            MoneyCalculator.roundedUpTripHomeMajor(
                                entities,
                                trip?.homeCurrencyCode ?: "EUR",
                            ),
                            trip?.homeCurrencyCode ?: "EUR",
                        ),
                    )
                    setTextViewText(
                        R.id.widget_exact_total,
                        context.getString(
                            R.string.widget_exact_and_count,
                            CurrencyAmount.formatMinor(
                                MoneyCalculator.exactTripHomeMinor(entities),
                                trip?.homeCurrencyCode ?: "EUR",
                            ),
                            entities.size,
                        ),
                    )
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent(context, ACTION_OPEN, 1))
                    setOnClickPendingIntent(R.id.widget_photo, pendingIntent(context, ACTION_PHOTO, 2))
                    setOnClickPendingIntent(R.id.widget_image, pendingIntent(context, ACTION_IMAGE, 3))
                    setOnClickPendingIntent(R.id.widget_manual, pendingIntent(context, ACTION_MANUAL, 4))
                    setOnClickPendingIntent(R.id.widget_statement, pendingIntent(context, ACTION_STATEMENT, 5))
                }
                manager.updateAppWidget(widgetId, views)
            }
        }

        private fun pendingIntent(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                Intent(context, MainActivity::class.java).apply {
                    this.action = action
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
