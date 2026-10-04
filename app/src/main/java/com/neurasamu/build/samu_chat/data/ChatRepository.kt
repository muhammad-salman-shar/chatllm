package com.neurasamu.build.samu_chat.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ChatRepository(ctx: Context) {
    private val db = AppDatabase.get(ctx)
    val apiDao = db.apiConfigDao()
    val convDao = db.conversationDao()
    val msgDao = db.messageDao()

    fun observeApis(): Flow<List<ApiConfig>> = apiDao.observeAll()
    suspend fun getApi(id: String) = apiDao.getById(id)
    suspend fun saveApi(c: ApiConfig) = apiDao.upsert(c)
    suspend fun deleteApi(c: ApiConfig) = apiDao.delete(c)

    fun observeConversations(): Flow<List<Conversation>> = convDao.observeAll()
    fun observeConversationsFor(apiId: String): Flow<List<Conversation>> =
        convDao.observeByApi(apiId)
    suspend fun getConversation(id: String) = convDao.getById(id)
    suspend fun saveConversation(c: Conversation) = convDao.upsert(c)
    suspend fun updateConversation(c: Conversation) = convDao.update(c)
    suspend fun deleteConversation(id: String) {
        msgDao.deleteByConversation(id)
        convDao.deleteById(id)
    }
    suspend fun renameConversation(id: String, title: String) = convDao.rename(id, title)

    fun observeMessages(convId: String): Flow<List<Message>> = msgDao.observeByConversation(convId)
    suspend fun listMessages(convId: String) = msgDao.listByConversation(convId)
    suspend fun insertMessage(m: Message) = msgDao.insert(m)
    suspend fun updateMessage(m: Message) = msgDao.update(m)
    suspend fun deleteMessage(id: String) = msgDao.deleteById(id)
}
