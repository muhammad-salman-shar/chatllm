package com.neurasamu.build.samu_chat

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.neurasamu.build.samu_chat.notifications.NotificationHelper

class SamuChatApplication : Application() {
    companion object {
        var isInBackground = false
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_STOP) {
                    isInBackground = true
                } else if (event == Lifecycle.Event.ON_START) {
                    isInBackground = false
                }
            }
        })
        NotificationHelper.createChannel(this)
    }
}
