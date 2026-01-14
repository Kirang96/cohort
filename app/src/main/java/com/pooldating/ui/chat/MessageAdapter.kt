package com.pooldating.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pooldating.R
import com.pooldating.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    fun submitList(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
    
    // Add a pending message for optimistic UI
    fun addPendingMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
    
    // Mark a pending message as failed
    fun markPendingMessageFailed(text: String) {
        val index = messages.indexOfLast { it.isPending && it.text == text }
        if (index != -1) {
            messages[index] = messages[index].copy(isPending = false, isFailed = true)
            notifyItemChanged(index)
        }
    }
    
    // Remove pending messages (called when server messages arrive)
    fun removePendingMessages() {
        val pendingRemoved = messages.removeAll { it.isPending || it.isFailed }
        if (pendingRemoved) notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender_id == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val view = inflater.inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvMessageText)
        private val tvTime: TextView = itemView.findViewById(R.id.tvMessageTime)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivMessageImage)

        fun bind(message: Message) {
            // Image Logic
            if (!message.image_url.isNullOrEmpty()) {
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.image_url)
                    .override(600, 600) // downsample for list
                    .into(ivImage)
            } else {
                ivImage.visibility = View.GONE
            }

            // Text Logic
            if (!message.text.isNullOrEmpty()) {
                tvText.visibility = View.VISIBLE
                tvText.text = message.text
            } else {
                tvText.visibility = View.GONE
            }
            
            // Show pending/failed state
            when {
                message.isFailed -> {
                    tvTime.text = "Failed ⚠️"
                    tvTime.setTextColor(android.graphics.Color.RED)
                }
                message.isPending -> {
                    tvTime.text = "Sending..."
                    tvTime.setTextColor(android.graphics.Color.GRAY)
                }
                else -> {
                    tvTime.text = formatTime(message.sent_at)
                    tvTime.setTextColor(android.graphics.Color.WHITE)
                }
            }
        }
        
        private fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
            if (timestamp == null) return ""
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate())
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvMessageText)
        private val tvTime: TextView = itemView.findViewById(R.id.tvMessageTime)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivMessageImage)

        fun bind(message: Message) {
            // Image Logic
            if (!message.image_url.isNullOrEmpty()) {
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.image_url)
                    .override(600, 600)
                    .into(ivImage)
            } else {
                ivImage.visibility = View.GONE
            }

            if (!message.text.isNullOrEmpty()) {
                tvText.visibility = View.VISIBLE
                tvText.text = message.text
            } else {
                tvText.visibility = View.GONE
            }
            
            tvTime.text = formatTime(message.sent_at)
        }
        
        private fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
            if (timestamp == null) return ""
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate())
        }
    }
}
