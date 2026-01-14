package com.pooldating.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.pooldating.R
import com.pooldating.data.repository.ChatRepository
import com.pooldating.data.model.Chat
import com.pooldating.utils.Result
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatsListActivity : AppCompatActivity() {

    private lateinit var repo: ChatRepository
    private lateinit var adapter: ChatAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tabLayout: TabLayout
    
    private var allChats = listOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats_list)

        repo = ChatRepository()
        
        setupUI()
        observeChats()
    }

    private fun setupUI() {
        tabLayout = findViewById(R.id.tabLayout)
        val rvChats = findViewById<RecyclerView>(R.id.rvChats)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = ChatAdapter { chat ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("match_id", chat.match_id)
            intent.putExtra("pool_id", chat.pool_id) // Optional
            startActivity(intent)
        }

        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = adapter

        tabLayout.addTab(tabLayout.newTab().setText("Active"))
        tabLayout.addTab(tabLayout.newTab().setText("Continued"))
        tabLayout.addTab(tabLayout.newTab().setText("Expired"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeChats() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repo.getMyChatsStream().collectLatest { result ->
                progressBar.visibility = View.GONE
                when (result) {
                    is Result.Success -> {
                        allChats = result.data
                        filterList()
                        updateBadges()
                    }
                    is Result.Error -> {
                        tvEmpty.text = "Error loading chats: ${result.exception.message}"
                        tvEmpty.visibility = View.VISIBLE
                    }
                    is Result.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun filterList() {
        val selectedTab = tabLayout.selectedTabPosition
        val now = System.currentTimeMillis()
        
        val filtered = when (selectedTab) {
            0 -> allChats.filter { chat ->
                // Active: status is "active" AND not expired
                chat.status == "active" && 
                    (chat.expires_at == null || chat.expires_at.toDate().time > now)
            }
            1 -> allChats.filter { it.status == "continued" }
            2 -> allChats.filter { chat ->
                // Expired: status is "expired" OR (status is "active" AND past expires_at)
                chat.status == "expired" || 
                    (chat.status == "active" && chat.expires_at != null && 
                     chat.expires_at.toDate().time <= now)
            }
            else -> allChats
        }
        
        adapter.submitList(filtered)
        
        tvEmpty.text = if (filtered.isEmpty()) "No chats found" else ""
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        
        // Update badges whenever list filters (or rather, when data updates)
        // But filterList is called on tab select too. Badges should persist.
        // It helps to refresh them.
    }
    
    private fun updateBadges() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        
        // Helper to check if chat is truly active (not expired)
        fun Chat.isTrulyActive(): Boolean = 
            status == "active" && (expires_at == null || expires_at.toDate().time > now)
        
        // Helper to check if chat is expired (status or time-based)
        fun Chat.isTrulyExpired(): Boolean =
            status == "expired" || (status == "active" && expires_at != null && expires_at.toDate().time <= now)
        
        val activeCount = allChats.count { chat ->
            chat.isTrulyActive() && (if (chat.user_a == uid) chat.unread_count_a else chat.unread_count_b) > 0 
        }
        val continuedCount = allChats.count { chat ->
            chat.status == "continued" && (if (chat.user_a == uid) chat.unread_count_a else chat.unread_count_b) > 0 
        }
        val expiredCount = allChats.count { chat ->
            chat.isTrulyExpired() && (if (chat.user_a == uid) chat.unread_count_a else chat.unread_count_b) > 0 
        }
        
        updateTabBadge(0, activeCount)
        updateTabBadge(1, continuedCount)
        updateTabBadge(2, expiredCount)
    }
    
    private fun updateTabBadge(index: Int, count: Int) {
        val tab = tabLayout.getTabAt(index) ?: return
        if (count > 0) {
            tab.getOrCreateBadge().number = count
        } else {
            tab.removeBadge()
        }
    }
}
