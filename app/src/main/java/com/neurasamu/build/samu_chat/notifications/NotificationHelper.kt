package com.neurasamu.build.samu_chat.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.neurasamu.build.samu_chat.MainActivity
import com.neurasamu.build.samu_chat.R
import com.neurasamu.build.samu_chat.data.ChatRepository
import com.neurasamu.build.samu_chat.data.Message
import com.neurasamu.build.samu_chat.network.ChatClient
import com.neurasamu.build.samu_chat.network.ChatEvent
import com.neurasamu.build.samu_chat.network.ChatRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object NotificationHelper {
    const val CHANNEL_ID = "chat_replies"
    const val KEY_TEXT_REPLY = "key_text_reply"
    const val ACTION_REPLY = "com.neurasamu.build.samu_chat.ACTION_REPLY"
    const val EXTRA_CONV_ID = "extra_conv_id"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Replies",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showReplyNotification(context: Context, convId: String, convTitle: String, replyText: String) {
        val notifId = convId.hashCode()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_CONV_ID, convId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).setLabel("Reply...").build()

        val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_CONV_ID, convId)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context, notifId, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground, "Reply", replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(convTitle)
            .setContentText(replyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(replyText))
            .setContentIntent(pendingIntent)
            .addAction(replyAction)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, builder.build())
    }

    fun updateNotificationProcessing(context: Context, notifId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Processing reply...")
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        manager.notify(notifId, builder.build())
    }
}

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_REPLY) {
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            val replyText = remoteInput?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString()
            val convId = intent.getStringExtra(NotificationHelper.EXTRA_CONV_ID)
            val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIF_ID, 0)

            if (!replyText.isNullOrBlank() && convId != null) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        NotificationHelper.updateNotificationProcessing(context, notifId)
                        val repo = ChatRepository(context)
                        val api = repo.observeApis().first().firstOrNull()
                        if (api != null) {
                            repo.insertMessage(Message(conversationId = convId, role = "user", content = replyText))
                            val msgs = repo.listMessages(convId)
                            val req = ChatRequest(
                                baseUrl = api.baseUrl, apiKey = api.apiKey, model = api.modelName,
                                messages = msgs.map { it.role to it.content },
                                temperature = api.temperature.toDouble(), maxTokens = api.maxTokensPerReply, stream = false
                            )
                            var fullText = ""
                            ChatClient.stream(req).collect { ev ->
                                if (ev is ChatEvent.Token) fullText += ev.text
                                else if (ev is ChatEvent.Done) {
                                    val content = ev.fullText.ifBlank { fullText }
                                    if (content.isNotBlank()) {
                                        repo.insertMessage(Message(conversationId = convId, role = "assistant", content = content))
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {} finally {
                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.cancel(notifId)
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
