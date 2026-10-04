package com.neurasamu.build.samu_chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.neurasamu.build.samu_chat.ui.SamuChatApp
import com.neurasamu.build.samu_chat.ui.theme.SamuChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SamuChatTheme {
                SamuChatApp()
            }
        }
    }
}
