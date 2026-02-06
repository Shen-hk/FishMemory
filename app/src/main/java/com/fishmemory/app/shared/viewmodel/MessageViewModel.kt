package com.fishmemory.app.shared.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fishmemory.app.data.model.MessageItem

class MessageViewModel : ViewModel() {

    private val _messageList = MutableLiveData<List<MessageItem>>(emptyList())
    val messageList: LiveData<List<MessageItem>> = _messageList

    init {
        loadMessages()
    }

    private fun loadMessages() {
        // 现在先写假数据，后面接接口 / 数据库
        _messageList.value = listOf(
            MessageItem(
                id = "1",
                title = "系统通知",
                lastMessage = "你有一条新的评论",
                time = "12:30"
            ),
            MessageItem(
                id = "2",
                title = "点赞提醒",
                lastMessage = "有人点赞了你的文章",
                time = "昨天"
            )
        )
    }
}
