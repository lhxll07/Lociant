package com.mnnode.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LociantAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
