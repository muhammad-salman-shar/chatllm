package com.neurasamu.build.samu_chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.neurasamu.build.samu_chat.data.AppSettings
import com.neurasamu.build.samu_chat.ui.SamuChatApp
import com.neurasamu.build.samu_chat.ui.theme.SamuChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var settings by remember { mutableStateOf(AppSettings.load(this)) }
            SamuChatTheme(settings = settings) {
                SamuChatApp(
                    settings = settings,
                    onSettingsChange = {
                        AppSettings.save(this, it)
                        settings = it
                    }
                )
            }
        }
    }
}
