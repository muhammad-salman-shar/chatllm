package com.neurasamu.build.samu_chat

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.neurasamu.build.samu_chat.notifications.NotificationHelper

class SamuChatApplication : Application(), DefaultLifecycleObserver {
    companion object {
        var isInBackground = false
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        NotificationHelper.createChannel(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        isInBackground = true
    }

    override fun onStart(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStart(owner)
        isInBackground = false
    }
}
