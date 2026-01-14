package com.pooldating.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pooldating.R
import com.pooldating.data.model.Chat
import android.graphics.Color

class ChatAdapter(private val onChatClick: (Chat) -> Unit) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val chats = mutableListOf<Chat>()

    fun submitList(newChats: List<Chat>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvChatName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvChatStatus)
        private val tvTime: TextView = itemView.findViewById(R.id.tvChatTime)
        private val tvUnreadBadge: TextView = itemView.findViewById(R.id.tvUnreadBadge)

        fun bind(chat: Chat) {
            // Determine current user to show other user's name
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isUserA = currentUserId == chat.user_a
            val otherUserName = if (isUserA) chat.user_b_name else chat.user_a_name
            
            // Unread Count Logic
            val unreadCount = if (isUserA) chat.unread_count_a else chat.unread_count_b // Note: user_a reads unread_count_a?
            // Wait, logic check:
            // Cloud Function: 
            // if (chat.user_a === auth.uid) updateData.unread_count_b = increment(1);
            // Sender A increments B.
            // So if I am B (recipient), I should read unread_count_b.
            // If I am A (recipient), I should read unread_count_a.
            // My code: if (isUserA) chat.unread_count_a else chat.unread_count_b.
            // Logic: am I User A? Yes. Then my unread count is unread_count_a.
            // Matches Cloud Function logic (Sender B increments A).
            // Logic is CORRECT.

            if (unreadCount > 0) {
                tvUnreadBadge.text = unreadCount.toString()
                tvUnreadBadge.visibility = View.VISIBLE
            } else {
                tvUnreadBadge.visibility = View.GONE
            }
            
            // Fallback for old chats or missing names
            val displayName = if (otherUserName.isNotEmpty() && otherUserName != "Unknown") otherUserName else "Match #${chat.match_id.takeLast(4)}"
            
            tvName.text = displayName
            
            val statusLabel = when {
                 chat.blocked_by.isNotEmpty() -> "Blocked"
                 chat.status == "active" -> "Active"
                 chat.status == "continued" -> "Continued"
                 chat.status == "expired" -> "Expired"
                 else -> chat.status
            }
            tvStatus.text = statusLabel
            
            // Timer Logic
            if (chat.blocked_by.isEmpty() && chat.status == "active" && chat.expires_at != null) {
                // ... (existing timer logic) ...
                val now = System.currentTimeMillis()
                val diff = chat.expires_at.toDate().time - now
                if (diff > 0) {
                    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff)
                    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                    tvTime.text = String.format("%dh %dm left", hours, minutes)
                    tvTime.visibility = View.VISIBLE
                } else {
                    tvTime.text = "Expiring..."
                    tvTime.visibility = View.VISIBLE
                }
            } else {
                tvTime.visibility = View.GONE
            }

            when {
                chat.blocked_by.isNotEmpty() -> tvStatus.setTextColor(Color.DKGRAY)
                chat.status == "active" -> tvStatus.setTextColor(Color.GREEN)
                chat.status == "expired" -> tvStatus.setTextColor(Color.RED)
                chat.status == "continued" -> tvStatus.setTextColor(Color.BLUE)
                else -> tvStatus.setTextColor(Color.GRAY) // Fallback for loading
            }

            itemView.setOnClickListener { onChatClick(chat) }
        }
    }
}
