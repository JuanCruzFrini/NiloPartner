package com.example.nilopartner.message

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nilopartner.R
import com.example.nilopartner.databinding.ItemChatBinding

class ChatAdapter(
    val messageList:MutableList<Message>,
    val listener: OnChatListener
)
    : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private lateinit var context:Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messageList[position]
        holder.setListener(message)

        var gravity = Gravity.END
        var background = ContextCompat.getDrawable(context, R.drawable.background_chat_support)
        var txtColor = ContextCompat.getColor(context, R.color.colorOnPrimary)

        val marginHorizontal = context.resources.getDimensionPixelSize(R.dimen.chat_margin_horizontal)
        val params = holder.binding.txtMessage.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = marginHorizontal
        params.marginEnd = 0
        params.topMargin = 0

        if (position > 0 && message.isSendByMe() != messageList[position - 1].isSendByMe()){
            params.topMargin = context.resources.getDimensionPixelSize(R.dimen.common_padding_min)
        }

        if (!message.isSendByMe()){
            gravity = Gravity.START
            background = ContextCompat.getDrawable(context, R.drawable.background_chat_client)
            txtColor = ContextCompat.getColor(context, R.color.colorOnSecondary)
            params.marginStart = 0
            params.marginEnd = marginHorizontal
        }
        holder.binding.root.gravity = gravity
        holder.binding.txtMessage.layoutParams = params
        holder.binding.txtMessage.background = background
        holder.binding.txtMessage.setTextColor(txtColor)

        holder.binding.txtMessage.text = message.message
    }

    override fun getItemCount(): Int = messageList.size

    fun addMessage(message: Message){
        if (!messageList.contains(message)){
            messageList.add(message)
            notifyItemInserted(messageList.size - 1)
        }
    }

    fun update(message: Message){
        val index = messageList.indexOf(message)
        if (index != -1){
            messageList.set(index, message)
            notifyItemChanged(index)
        }
    }

    fun delete(message: Message){
        val index =  messageList.indexOf(message)
        if (index != -1){
            messageList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = ItemChatBinding.bind(itemView)

        fun setListener(message: Message){
            binding.txtMessage.setOnLongClickListener {
                listener.deleteMessage(message)
                true
            }
        }
    }
}