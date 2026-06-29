package com.example.timeapk.permissions

import android.annotation.SuppressLint
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

private const val PERMISSION_PREFS = "permission_prompt_state"
private const val KEY_NOTIFICATION_REQUESTED = "notification_requested"
private const val KEY_CALENDAR_REQUESTED = "calendar_requested"

fun Context.areAppNotificationsEnabledCompat(): Boolean {
    return NotificationManagerCompat.from(this).areNotificationsEnabled()
}

fun Context.canPostAppNotifications(): Boolean {
    if (!areAppNotificationsEnabledCompat()) {
        return false
    }
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        hasPermission(notificationRuntimePermissionName())
}

fun Context.hasNotificationRuntimePermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        hasPermission(notificationRuntimePermissionName())
}

@SuppressLint("InlinedApi")
fun notificationRuntimePermissionName(): String = Manifest.permission.POST_NOTIFICATIONS

fun Context.hasCalendarReadWritePermission(): Boolean {
    return hasPermission(Manifest.permission.READ_CALENDAR) &&
        hasPermission(Manifest.permission.WRITE_CALENDAR)
}

fun Context.didGrantNotificationPermissionAfterRequest(callbackGranted: Boolean): Boolean {
    return isNotificationPermissionGrantedAfterRequest(
        callbackGranted = callbackGranted,
        hasRuntimePermission = hasNotificationRuntimePermission()
    )
}

fun Context.didGrantCalendarPermissionAfterRequest(
    grantResults: Map<String, Boolean>
): Boolean {
    return areCalendarPermissionsGrantedAfterRequest(
        grantResults = grantResults,
        hasReadPermission = hasPermission(Manifest.permission.READ_CALENDAR),
        hasWritePermission = hasPermission(Manifest.permission.WRITE_CALENDAR)
    )
}

fun Context.shouldShowNotificationPermissionRationaleCompat(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationRuntimePermission()) {
        return false
    }
    val activity = findActivityForPermissions() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        notificationRuntimePermissionName()
    )
}

fun Context.shouldShowCalendarPermissionRationaleCompat(): Boolean {
    if (hasCalendarReadWritePermission()) {
        return false
    }
    val activity = findActivityForPermissions() ?: return false
    val needsRead = !hasPermission(Manifest.permission.READ_CALENDAR)
    val needsWrite = !hasPermission(Manifest.permission.WRITE_CALENDAR)
    return (needsRead && ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.READ_CALENDAR
    )) || (needsWrite && ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.WRITE_CALENDAR
    ))
}

fun Context.wasNotificationPermissionRequestedBefore(): Boolean {
    return permissionPrefs().getBoolean(KEY_NOTIFICATION_REQUESTED, false)
}

fun Context.wasCalendarPermissionRequestedBefore(): Boolean {
    return permissionPrefs().getBoolean(KEY_CALENDAR_REQUESTED, false)
}

fun Context.markNotificationPermissionRequested() {
    permissionPrefs().edit { putBoolean(KEY_NOTIFICATION_REQUESTED, true) }
}

fun Context.markCalendarPermissionRequested() {
    permissionPrefs().edit { putBoolean(KEY_CALENDAR_REQUESTED, true) }
}

fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        putExtra("app_package", packageName)
        putExtra("app_uid", applicationInfo.uid)
    }
    launchExternalSettingsIntent(intent)
}

fun Context.openAppDetailsSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    launchExternalSettingsIntent(intent)
}

fun Context.openSystemSyncSettings() {
    val syncIntent = Intent(Settings.ACTION_SYNC_SETTINGS)
    val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
    val opened = runCatching {
        launchExternalSettingsIntent(syncIntent)
        true
    }.getOrDefault(false)
    if (!opened) {
        runCatching { launchExternalSettingsIntent(fallbackIntent) }
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

internal fun isNotificationPermissionGrantedAfterRequest(
    callbackGranted: Boolean,
    hasRuntimePermission: Boolean
): Boolean {
    return callbackGranted || hasRuntimePermission
}

internal fun areCalendarPermissionsGrantedAfterRequest(
    grantResults: Map<String, Boolean>,
    hasReadPermission: Boolean,
    hasWritePermission: Boolean
): Boolean {
    val readGranted = grantResults[Manifest.permission.READ_CALENDAR] ?: hasReadPermission
    val writeGranted = grantResults[Manifest.permission.WRITE_CALENDAR] ?: hasWritePermission
    return readGranted && writeGranted
}

internal fun buildExternalSettingsLaunchFlags(): Int {
    return Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
}

private fun Context.permissionPrefs() =
    getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)

private fun Context.launchExternalSettingsIntent(intent: Intent) {
    intent.addFlags(buildExternalSettingsLaunchFlags())
    startActivity(intent)
}

private fun Context.findActivityForPermissions(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}
