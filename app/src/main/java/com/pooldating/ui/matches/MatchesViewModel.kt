package com.pooldating.ui.matches

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pooldating.data.model.MatchWithUser
import com.pooldating.data.repository.MatchesRepository
import com.pooldating.utils.Result
import kotlinx.coroutines.launch

class MatchesViewModel : ViewModel() {
    
    private val repository = MatchesRepository()
    
    private val _matches = MutableLiveData<Result<List<MatchWithUser>>>()
    val matches: LiveData<Result<List<MatchWithUser>>> = _matches
    
    fun loadMatches(poolId: String) {
        _matches.value = Result.Loading
        viewModelScope.launch {
            val result = repository.getMatchesForPool(poolId)
            _matches.value = result
        }
    }
}
