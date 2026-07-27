package com.gembyte.poslite.components.printer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothPermissionHelper {

    fun hasBluetoothPermissions(
        context: Context
    ): Boolean {

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {
            return true
        }

        val connect =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) ==
                    PackageManager.PERMISSION_GRANTED

        val scan =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) ==
                    PackageManager.PERMISSION_GRANTED

        return connect && scan
    }
}