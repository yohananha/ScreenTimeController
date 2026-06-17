package com.screentime.tv

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.screentime.tv.service.EnforcementAccessibilityService
import com.screentime.tv.usage.UsagePermission

data class PermissionState(
    val usageAccess: Boolean,
    val overlay: Boolean,
    val accessibility: Boolean,
) {
    val allGranted: Boolean get() = usageAccess && overlay && accessibility

    companion object {
        fun read(context: Context): PermissionState = PermissionState(
            usageAccess = UsagePermission.isGranted(context),
            overlay = canDrawOverlays(context),
            accessibility = isAccessibilityEnabled(context),
        )

        private fun canDrawOverlays(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true

        private fun isAccessibilityEnabled(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as AccessibilityManager
            val enabled = manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
            )
            val ourComponent = ComponentName(context, EnforcementAccessibilityService::class.java)
            return enabled.any { info ->
                val service = info.resolveInfo.serviceInfo
                service.packageName == ourComponent.packageName && service.name == ourComponent.className
            }
        }
    }
}
