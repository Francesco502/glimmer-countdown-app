package com.example.timeapk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.home.toEventUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * 单事件大数字小组件：显示一个选定事件（或最近事件）的天数。
 * 背景、圆角、边框与文字对比沿用“默认配置”，实例只保存选中的事件。
 */
class SingleEventWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val REQUEST_CODE_BASE = 60_000

        fun getAppWidgetIds(
            context: Context,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
        ): IntArray = appWidgetManager.getAppWidgetIds(
            ComponentName(context, SingleEventWidgetProvider::class.java)
        )

        fun refreshAll(context: Context) {
            val app = context.applicationContext as? TimeApplication ?: return
            app.launchAppTask { refreshAllAndAwait(app) }
        }

        suspend fun refreshAllAndAwait(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = getAppWidgetIds(context, appWidgetManager)
            if (ids.isEmpty()) return
            withContext(Dispatchers.IO) { refresh(context, appWidgetManager, ids) }
        }

        internal suspend fun refresh(context: Context, appWidgetManager: AppWidgetManager, ids: IntArray) {
            WidgetRefreshCoordinator.runLatestSnapshot {
                refreshLatestSnapshot(context, appWidgetManager, ids)
            }
        }

        private suspend fun refreshLatestSnapshot(context: Context, appWidgetManager: AppWidgetManager, ids: IntArray) {
            val app = context.applicationContext as? TimeApplication ?: return
            val prefs = app.userPrefs
            val milestones = prefs.customMilestonesFlow.first()
            val smartMilestones = prefs.smartMilestonesEnabledFlow.first()
            val pinned = prefs.pinnedEventIdsFlow.first()
            val states = app.repository.getAllEventsSnapshot().map { it.toEventUiState(milestones, smartMilestones) }
            val repository = WidgetConfigRepository(context)
            val config = repository.getDefaultConfig().sanitize()
            val theme = WidgetThemeResolver.resolve(context)
            val today = LocalDate.now()
            val localizedContext = widgetLanguageContext(context)
            ids.forEach { appWidgetId ->
                val selection = repository.getSingleEventSelection(appWidgetId)
                val target = SingleEventWidgetPolicy.resolveTarget(states, selection, today, pinned)
                val text = target?.let { SingleEventWidgetPolicy.buildText(localizedContext, it) }
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
                val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                appWidgetManager.updateAppWidget(
                    appWidgetId,
                    buildViews(localizedContext, appWidgetId, text, config, theme, widthDp, heightDp)
                )
            }
        }

        internal fun buildViews(
            context: Context,
            appWidgetId: Int,
            text: SingleEventWidgetText?,
            config: WidgetConfig,
            theme: WidgetThemeSnapshot,
            widthDp: Int,
            heightDp: Int
        ): RemoteViews {
            val style = WidgetRenderPolicy.resolve(config, theme)
            val layout = WidgetRenderPolicy.resolveSingleEventLayoutResId(config)
            return RemoteViews(context.packageName, layout).apply {
                setWidgetBackground(style)
                setWidgetTextColors(
                    context, style,
                    primaryIds = intArrayOf(R.id.single_unit, R.id.single_title),
                    secondaryIds = intArrayOf(R.id.single_label, R.id.single_date),
                    accentIds = intArrayOf(R.id.single_number)
                )
                val scale = config.fontScale
                setTextViewTextSize(
                    R.id.single_number,
                    TypedValue.COMPLEX_UNIT_SP,
                    if (text?.isToday == true) {
                        SingleEventWidgetPolicy.todayTextSp(context, text.number, widthDp, heightDp, scale)
                    } else {
                        SingleEventWidgetPolicy.numberTextSp(widthDp, heightDp) * scale
                    }
                )
                setTextViewTextSize(R.id.single_title, TypedValue.COMPLEX_UNIT_SP, 14f * scale)
                if (text == null) {
                    setContentDescription(R.id.widget_root, context.getString(R.string.home_empty_title))
                    setTextViewText(R.id.single_label, "")
                    setTextViewText(R.id.single_number, "")
                    setTextViewText(R.id.single_unit, "")
                    setTextViewText(R.id.single_title, context.getString(R.string.home_empty_title))
                    setViewVisibility(R.id.single_date, View.GONE)
                } else {
                    setContentDescription(
                        R.id.widget_root,
                        "${text.title}, ${text.label} ${text.number} ${text.unit}, ${text.dateText}"
                    )
                    setTextViewText(R.id.single_label, text.label)
                    setViewVisibility(R.id.single_label, if (text.label.isEmpty()) View.GONE else View.VISIBLE)
                    setTextViewText(R.id.single_number, text.number)
                    setTextViewText(R.id.single_unit, text.unit)
                    setViewVisibility(R.id.single_unit, if (text.unit.isEmpty()) View.GONE else View.VISIBLE)
                    setTextViewText(R.id.single_title, text.title)
                    setTextViewText(R.id.single_date, text.dateText)
                    setViewVisibility(
                        R.id.single_date,
                        if (SingleEventWidgetPolicy.showsDate(widthDp, heightDp)) View.VISIBLE else View.GONE
                    )
                }
                setOnClickPendingIntent(R.id.widget_root, openIntent(context, appWidgetId, text?.eventId))
            }
        }

        private fun openIntent(context: Context, appWidgetId: Int, eventId: Int?): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                if (eventId != null) putExtra("open_event_id", eventId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getActivity(context, REQUEST_CODE_BASE + appWidgetId, intent, flags)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetDateBoundaryScheduler.scheduleOrCancel(context)
        val pending = goAsync()
        val app = context.applicationContext as? TimeApplication
        if (app == null) {
            pending.finish()
            return
        }
        app.launchAppTask {
            try {
                withContext(Dispatchers.IO) { refresh(app, appWidgetManager, appWidgetIds) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetDateBoundaryScheduler.scheduleOrCancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetDateBoundaryScheduler.scheduleOrCancel(context)
        val app = context.applicationContext as? TimeApplication ?: return
        app.launchAppTask {
            val repository = WidgetConfigRepository(app)
            appWidgetIds.forEach { repository.removeSingleEventSelection(it) }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetDateBoundaryScheduler.scheduleOrCancel(context)
    }
}
