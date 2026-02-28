package com.example.timeapk.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Extension method to find the Activity from a Context.
 * This is useful when the context is wrapped (e.g., by ContextThemeWrapper or LocaleUtils).
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}
