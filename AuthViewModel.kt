package com.studypulse.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.app.data.auth.AuthRepository
import com.studypulse.app.data.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isError: Boolean = false,
    val message: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { _uiState.update { it.copy(isError=true, message="⚠️ Please fill in all fields.") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading=true, message="") }
            when (val r = authRepository.signIn(email, password)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading=false, isSuccess=true, message="✅ Welcome back!") }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading=false, isError=true, message="❌ ${r.message}") }
                else -> {}
            }
        }
    }

    fun register(firstName: String, lastName: String, email: String, password: String, year: Int, programme: String) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) { _uiState.update { it.copy(isError=true, message="⚠️ Please fill in all fields.") }; return }
        if (password.length < 8) { _uiState.update { it.copy(isError=true, message="❌ Password must be at least 8 characters.") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading=true, message="") }
            when (val r = authRepository.register(firstName, lastName, email, password, year, programme)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading=false, isSuccess=true, message="✅ Account created!") }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading=false, isError=true, message="❌ ${r.message}") }
                else -> {}
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) { _uiState.update { it.copy(isError=true, message="⚠️ Enter your email first.") }; return }
        viewModelScope.launch {
            when (authRepository.sendPasswordReset(email)) {
                is AuthResult.Success -> _uiState.update { it.copy(isError=false, message="📧 Password reset sent to $email") }
                else -> _uiState.update { it.copy(isError=true, message="❌ Could not send reset email.") }
            }
        }
    }

    fun signInWithGoogle() { _uiState.update { it.copy(message="🔄 Redirecting to Google...") } }
    fun clearState() { _uiState.update { AuthUiState() } }
}
