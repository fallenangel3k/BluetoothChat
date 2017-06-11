package com.glodanif.bluetoothchat.database

import android.arch.persistence.room.*
import com.glodanif.bluetoothchat.entity.Conversation

@Dao
interface ConversationsDao {

    @Query("SELECT * FROM conversation")
    fun getAllConversations(): List<Conversation>

    @Query("SELECT * FROM conversation LEFT JOIN message ON conversation.address = message.address AND message.date = (SELECT MAX(message.date) FROM message WHERE conversation.address = message.address) GROUP BY message.date")
    fun getAllConversationsWithMessages(): List<Conversation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(conversations: Conversation)

    @Delete
    fun delete(conversations: Conversation)
}
