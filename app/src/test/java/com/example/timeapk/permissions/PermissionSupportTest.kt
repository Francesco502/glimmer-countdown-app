package com.example.timeapk.permissions

import android.Manifest
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionSupportTest {

    @Test
    fun isNotificationPermissionGrantedAfterRequest_acceptsCurrentRuntimeState() {
        assertTrue(
            isNotificationPermissionGrantedAfterRequest(
                callbackGranted = false,
                hasRuntimePermission = true
            )
        )
    }

    @Test
    fun areCalendarPermissionsGrantedAfterRequest_fallsBackToAlreadyGrantedPermissions() {
        val grantResults = mapOf(
            Manifest.permission.WRITE_CALENDAR to true
        )

        assertTrue(
            areCalendarPermissionsGrantedAfterRequest(
                grantResults = grantResults,
                hasReadPermission = true,
                hasWritePermission = true
            )
        )
    }

    @Test
    fun areCalendarPermissionsGrantedAfterRequest_requiresBothPermissions() {
        val grantResults = mapOf(
            Manifest.permission.READ_CALENDAR to true,
            Manifest.permission.WRITE_CALENDAR to false
        )

        assertFalse(
            areCalendarPermissionsGrantedAfterRequest(
                grantResults = grantResults,
                hasReadPermission = false,
                hasWritePermission = false
            )
        )
    }

    @Test
    fun buildExternalSettingsLaunchFlags_opensSettingsOutsideAppTask() {
        val flags = buildExternalSettingsLaunchFlags()

        assertTrue(flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(flags and Intent.FLAG_ACTIVITY_MULTIPLE_TASK != 0)
    }
}
