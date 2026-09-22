package com.robopal.app.services

import android.inputmethodservice.InputMethodService

class RobotImeService : InputMethodService() {

    companion object {
        var pendingText: String? = null
        var instance: RobotImeService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val textToCommit = pendingText
        if (!textToCommit.isNullOrEmpty()) {
            val ic = currentInputConnection
            if (ic != null) {
                ic.commitText(textToCommit, 1)
                pendingText = null
                requestHideSelf(0)
            }
        }
    }
}
