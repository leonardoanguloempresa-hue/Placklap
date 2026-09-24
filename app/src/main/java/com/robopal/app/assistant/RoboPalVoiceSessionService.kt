package com.robopal.app.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class RoboPalVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return RoboPalVoiceSession(this)
    }
}

class RoboPalVoiceSession(context: VoiceInteractionSessionService) : VoiceInteractionSession(context)
