package com.pooldating.ui.chat

import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.pooldating.R
import com.pooldating.data.model.Chat
import com.pooldating.data.model.Message
import com.pooldating.utils.Result
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.google.firebase.Timestamp

class ChatActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: MessageAdapter
    private var matchId: String = ""
    private var currentChat: Chat? = null
    
    // UI Elements
    private lateinit var tvTimer: TextView
    private lateinit var tvChatTitle: TextView
    private lateinit var layoutContinue: LinearLayout
    private lateinit var tvContinueStatus: TextView
    private lateinit var btnContinueChat: Button
    private lateinit var layoutInput: LinearLayout
    private lateinit var tvExpiredNotice: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var rvMessages: RecyclerView

    private var countdownTimer: CountDownTimer? = null
    private var otherUid: String = ""
    private var otherName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(android.content.Intent(this, com.pooldating.ui.login.LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_chat)

        matchId = intent.getStringExtra("match_id") ?: return finish()
        
        val factory = ChatViewModelFactory()
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]
        
        initViews()
        
        viewModel.loadChat(matchId)
        
        observeViewModel()
    }

    private fun initViews() {
        tvTimer = findViewById(R.id.tvTimer)
        tvChatTitle = findViewById(R.id.tvChatTitle)
        layoutContinue = findViewById(R.id.layoutContinue)
        tvContinueStatus = findViewById(R.id.tvContinueStatus)
        btnContinueChat = findViewById(R.id.btnContinueChat)
        layoutInput = findViewById(R.id.layoutInput)
        tvExpiredNotice = findViewById(R.id.tvExpiredNotice)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        rvMessages = findViewById(R.id.rvMessages)

        adapter = MessageAdapter()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // Messages from bottom
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter

        // Image Picker
        val pickMedia = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.uploadImage(contentResolver, uri, matchId)
            }
        }
        
        // Find View (Note: btnAttach might not exist if using view binding? No, using findViewById)
        val btnAttach = findViewById<android.widget.ImageButton>(R.id.btnAttach)
        btnAttach.setOnClickListener {
             pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnSend.setOnClickListener { sendMessage() }
        btnContinueChat.setOnClickListener { 
            viewModel.requestContinuation(matchId)
            btnContinueChat.isEnabled = false
            btnContinueChat.text = getString(R.string.requesting)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.chatState.collectLatest { result ->
                if (result is Result.Success) {
                    val chat = result.data
                    currentChat = chat
                    updateUI(chat)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.messagesState.collectLatest { result ->
                if (result is Result.Success) {
                    adapter.removePendingMessages()
                    adapter.submitList(result.data)
                    if (result.data.isNotEmpty()) {
                        rvMessages.scrollToPosition(result.data.size - 1)
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.operationStatus.collectLatest { status ->
                status?.let {
                    Toast.makeText(this@ChatActivity, it, Toast.LENGTH_SHORT).show()
                    if (it == "User blocked") finish()
                    viewModel.clearStatus()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            val isBlocked = currentChat?.blocked_by?.contains(FirebaseAuth.getInstance().currentUser?.uid) == true
            it.findItem(R.id.action_block)?.isVisible = !isBlocked
            it.findItem(R.id.action_unblock)?.isVisible = isBlocked
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_report -> {
                showReportDialog()
                true
            }
            R.id.action_block -> {
                showBlockDialog()
                true
            }
            R.id.action_unblock -> {
                viewModel.unblockUser(otherUid, matchId)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showBlockDialog() {
        if (otherUid.isEmpty()) return
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.block_confirm_title, otherName.ifEmpty { "User" }))
            .setMessage(getString(R.string.block_confirm_message))
            .setPositiveButton(getString(R.string.block_action)) { _, _ ->
                viewModel.blockUser(otherUid, matchId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showReportDialog() {
        if (otherUid.isEmpty()) return
        
        val options = arrayOf("Spam", "Fake Profile", "Harassment", "Abuse", "Other")
        var selectedIndex = 0
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.report_title))
            .setSingleChoiceItems(options, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(getString(R.string.report_button)) { _, _ ->
                val reasonKey = when(selectedIndex) {
                    0 -> "spam"
                    1 -> "fake_profile"
                    2 -> "harassment"
                    3 -> "abuse"
                    else -> "other"
                }
                viewModel.reportUser(otherUid, reasonKey, matchId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateUI(chat: Chat) {
        invalidateOptionsMenu() // Update menu visibility

        // Set Chat Title based on other user's name (denormalized in Firestore)
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val isUserA = myUid == chat.user_a
        otherName = if (isUserA) chat.user_b_name else chat.user_a_name
        otherUid = if (isUserA) chat.user_b else chat.user_a
        
        tvChatTitle.text = if (otherName.isNotEmpty() && otherName != "Unknown") otherName else "Chat"
        
        // CHECK BLOCKED STATUS
        if (chat.blocked_by.isNotEmpty()) {
            layoutInput.visibility = View.GONE
            layoutContinue.visibility = View.GONE
            tvExpiredNotice.visibility = View.VISIBLE
            tvExpiredNotice.text = getString(R.string.chat_blocked_input)
            tvTimer.text = "BLOCKED"
            stopTimer()
            return
        }

        // Status Handling
        when (chat.status) {
            "active" -> {
                layoutInput.visibility = View.VISIBLE
                tvExpiredNotice.visibility = View.GONE
                startTimer(chat.expires_at)
                
                // Continuation Logic
                val hasMe = chat.continued_by.contains(myUid)
                val hasOthers = chat.continued_by.any { it != myUid }
                
                layoutContinue.visibility = View.VISIBLE
                if (hasMe) {
                    btnContinueChat.visibility = View.GONE
                    tvContinueStatus.text = getString(R.string.chat_waiting)
                    tvContinueStatus.visibility = View.VISIBLE
                } else {
                    btnContinueChat.visibility = View.VISIBLE
                    tvContinueStatus.visibility = View.GONE
                    
                    if (hasOthers) {
                        btnContinueChat.text = getString(R.string.accept_chat)
                        tvContinueStatus.text = getString(R.string.partner_continue)
                        tvContinueStatus.visibility = View.VISIBLE
                    } else {
                        btnContinueChat.text = getString(R.string.continue_chat)
                        tvContinueStatus.visibility = View.GONE
                    }
                }
            }
            "continued" -> {
                layoutInput.visibility = View.VISIBLE
                tvExpiredNotice.visibility = View.GONE
                layoutContinue.visibility = View.GONE
                tvTimer.text = "∞"
                stopTimer()
            }
            "expired" -> {
                layoutInput.visibility = View.GONE
                layoutContinue.visibility = View.GONE
                tvExpiredNotice.visibility = View.VISIBLE
                tvExpiredNotice.text = "Chat has expired."
                tvTimer.text = "EXPIRED"
                stopTimer()
            }
            else -> {
                layoutInput.visibility = View.GONE
                layoutContinue.visibility = View.GONE
            }
        }
    }

    private fun startTimer(expiry: Timestamp?) {
        if (countdownTimer != null) return // Already running
        if (expiry == null) return

        val now = System.currentTimeMillis()
        val end = expiry.toDate().time
        val diff = end - now

        if (diff <= 0) {
            tvTimer.text = "EXPIRED"
            return
        }

        countdownTimer = object : CountDownTimer(diff, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val h = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val m = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val s = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                tvTimer.text = String.format("%02d:%02d:%02d", h, m, s)
            }

            override fun onFinish() {
                tvTimer.text = "EXPIRED"
                layoutInput.visibility = View.GONE
                tvExpiredNotice.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun stopTimer() {
        countdownTimer?.cancel()
        countdownTimer = null
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // OPTIMISTIC UI: Add message immediately to adapter (Visual only)
        val pendingMessage = Message(
            sender_id = myUid,
            text = text,
            sent_at = Timestamp.now(),
            isPending = true
        )
        adapter.addPendingMessage(pendingMessage)
        rvMessages.scrollToPosition(adapter.itemCount - 1)
        
        etMessage.setText("")
        
        viewModel.sendMessage(matchId, text, myUid)
    }
}
