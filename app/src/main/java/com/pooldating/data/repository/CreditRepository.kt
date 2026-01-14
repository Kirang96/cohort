package com.pooldating.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.pooldating.utils.Result
import kotlinx.coroutines.tasks.await

class CreditRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    
    /**
     * Get current user's credit balance from server.
     * Balance is ALWAYS computed server-side from the ledger - never stored locally.
     */
    suspend fun getCreditBalance(): Result<Int> {
        val user = auth.currentUser ?: return Result.Error(Exception("Not logged in"))
        
        return try {
            val result = functions
                .getHttpsCallable("getUserCreditBalance")
                .call()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any>
            val balance = (data?.get("balance") as? Number)?.toInt() ?: 0
            
            Result.Success(balance)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Mock purchase credits for testing.
     * TODO: Replace with real payment gateway integration.
     */
    suspend fun purchaseCredits(amount: Int = 5): Result<PurchaseResult> {
        val user = auth.currentUser ?: return Result.Error(Exception("Not logged in"))
        
        return try {
            val result = functions
                .getHttpsCallable("mockPurchaseCredits")
                .call(hashMapOf("amount" to amount))
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any>
            
            val purchaseResult = PurchaseResult(
                creditsAdded = (data?.get("credits_added") as? Number)?.toInt() ?: 0,
                newBalance = (data?.get("new_balance") as? Number)?.toInt() ?: 0,
                referenceId = data?.get("reference_id") as? String ?: ""
            )
            
            Result.Success(purchaseResult)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    data class PurchaseResult(
        val creditsAdded: Int,
        val newBalance: Int,
        val referenceId: String
    )
}
