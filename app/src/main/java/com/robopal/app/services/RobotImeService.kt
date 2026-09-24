package com.robopal.app.services

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import com.robopal.app.R

class RobotImeService : InputMethodService() {

    companion object {
        private const val TAG = "RobotImeService"
        var instance: RobotImeService? = null
        var pendingText: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "RobotImeService creado")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.ime_keyboard_view, null)
    }

    fun injectText(text: String): Boolean {
        val sanitized = text.replace("\r", "")
        val ic = currentInputConnection ?: run {
            Log.w(TAG, "No hay InputConnection activo para inyectar texto")
            return false
        }
        return ic.commitText(sanitized, 1)
    }
}
