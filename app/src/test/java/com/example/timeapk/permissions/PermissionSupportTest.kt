package com.example.timeapk.permissions

import android.Manifest
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
}
