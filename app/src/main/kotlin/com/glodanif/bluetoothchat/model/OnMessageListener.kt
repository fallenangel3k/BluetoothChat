package com.glodanif.bluetoothchat.model

import com.glodanif.bluetoothchat.entity.ChatMessage

interface OnMessageListener {
    fun onMessageReceived(message: ChatMessage)
    fun onMessageSent(message: ChatMessage)
}