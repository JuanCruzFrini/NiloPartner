package com.example.nilopartner.message

import com.example.nilopartner.message.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}