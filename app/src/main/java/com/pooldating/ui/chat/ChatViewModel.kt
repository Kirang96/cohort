package com.pooldating.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.pooldating.data.model.Chat
import com.pooldating.data.model.Message
import com.pooldating.data.repository.ChatRepository
import com.pooldating.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _chatState = MutableStateFlow<Result<Chat>>(Result.Loading)
    val chatState: StateFlow<Result<Chat>> = _chatState.asStateFlow()

    private val _messagesState = MutableStateFlow<Result<List<Message>>>(Result.Loading)
    val messagesState: StateFlow<Result<List<Message>>> = _messagesState.asStateFlow()

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    fun loadChat(matchId: String) {
        viewModelScope.launch {
            repository.getChatStream(matchId).collect {
                _chatState.value = it
            }
        }
        viewModelScope.launch {
            repository.getMessagesStream(matchId).collect {
                _messagesState.value = it
            }
        }
        viewModelScope.launch {
            repository.markChatRead(matchId)
        }
    }

    fun sendMessage(matchId: String, text: String, myUid: String) {
        // Optimistic update handled by UI/Adapter usually, but we can do it here if we want complex state.
        // For now, fire and forget (repository Result will be logged or handled).
        viewModelScope.launch {
            val result = repository.sendMessage(matchId, text)
            if (result is Result.Error) {
                _operationStatus.value = "Failed to send: ${result.exception.message}"
            }
        }
    }

    fun requestContinuation(matchId: String) {
        viewModelScope.launch {
            val result = repository.requestChatContinuation(matchId)
            if (result is Result.Error) {
                _operationStatus.value = "Failed to continue: ${result.exception.message}"
            }
        }
    }

    fun blockUser(otherUid: String, matchId: String) {
        viewModelScope.launch {
            val result = repository.blockUser(otherUid, "In-app block", matchId)
            if (result is Result.Success) {
                _operationStatus.value = "User blocked"
            } else if (result is Result.Error) {
                _operationStatus.value = "Block failed: ${result.exception.message}"
            }
        }
    }

    fun unblockUser(otherUid: String, matchId: String) {
        viewModelScope.launch {
            val result = repository.unblockUser(otherUid, matchId)
            if (result is Result.Success) {
                _operationStatus.value = "User unblocked"
            } else if (result is Result.Error) {
                _operationStatus.value = "Unblock failed: ${result.exception.message}"
            }
        }
    }
    
    fun reportUser(otherUid: String, reason: String, matchId: String) {
         viewModelScope.launch {
            val result = repository.reportUser(otherUid, reason, null, matchId)
            if (result is Result.Success) {
                _operationStatus.value = "Report received"
            } else if (result is Result.Error) {
                _operationStatus.value = "Report failed: ${result.exception.message}"
            }
        }
    }

    fun clearStatus() {
        _operationStatus.value = null
    }
    // ============================================
    // IMAGE UPLOAD LOGIC
    // ============================================
    fun uploadImage(contentResolver: android.content.ContentResolver, uri: android.net.Uri, matchId: String) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "=== IMAGE UPLOAD START ===")
            android.util.Log.d("ChatViewModel", "MatchId: $matchId, Uri: $uri")
            _operationStatus.value = "Preparing upload..."
            
            // 1. Request storage path from backend (validates permissions)
            android.util.Log.d("ChatViewModel", "Step 1: Calling requestImageUploadUrl...")
            val uploadInfoResult = repository.requestImageUploadUrl(matchId)
            android.util.Log.d("ChatViewModel", "Step 1: Result type = ${uploadInfoResult::class.simpleName}")
            
            if (uploadInfoResult is Result.Error) {
                val ex = uploadInfoResult.exception
                val errorDetail = "STEP1_FAIL: ${ex::class.simpleName}: ${ex.message}"
                android.util.Log.e("ChatViewModel", "Step 1 FAILED: $errorDetail", ex)
                _operationStatus.value = errorDetail
                return@launch
            }
            val uploadInfo = (uploadInfoResult as Result.Success).data
            android.util.Log.d("ChatViewModel", "Step 1 SUCCESS: storagePath=${uploadInfo.storagePath}, downloadUrl=${uploadInfo.downloadUrl}")
            
            // 2. Upload File using Firebase Storage SDK (uses Firebase Auth, no IAM needed)
            _operationStatus.value = "Uploading image..."
            android.util.Log.d("ChatViewModel", "Step 2: Starting Firebase Storage upload...")
            var actualDownloadUrl: String = ""
            try {
                val storageRef = FirebaseStorage.getInstance()
                    .reference.child(uploadInfo.storagePath)
                android.util.Log.d("ChatViewModel", "Step 2: StorageRef created: ${storageRef.path}")
                
                // Upload and wait for completion
                storageRef.putFile(uri).await()
                
                // Get the actual download URL (includes access token)
                actualDownloadUrl = storageRef.downloadUrl.await().toString()
                
                android.util.Log.d("ChatViewModel", "Step 2 SUCCESS: Upload complete. Download URL: $actualDownloadUrl")
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Step 2 FAILED: ${e.message}", e)
                _operationStatus.value = "Upload failed: ${e.message}"
                return@launch
            }

            // 3. Send Message linked to Image (use the actual download URL, not the pre-constructed one)
            _operationStatus.value = "Sending image..."
            android.util.Log.d("ChatViewModel", "Step 3: Calling sendImageMessage with URL: $actualDownloadUrl")
            val sendResult = repository.sendImageMessage(matchId, actualDownloadUrl)
            if (sendResult is Result.Success) {
                android.util.Log.d("ChatViewModel", "Step 3 SUCCESS: Image message sent!")
                _operationStatus.value = null // Success (silent)
            } else if (sendResult is Result.Error) {
                android.util.Log.e("ChatViewModel", "Step 3 FAILED: ${(sendResult as Result.Error).exception.message}")
                _operationStatus.value = "Failed to send image msg: ${sendResult.exception.message}"
            }
            android.util.Log.d("ChatViewModel", "=== IMAGE UPLOAD END ===")
        }
    }
}

class ChatViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(ChatRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
