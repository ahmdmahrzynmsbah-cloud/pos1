package com.example.ui

sealed class AuthState {
  object Idle : AuthState()
  object Loading : AuthState()
  data class Success(val role: String, val name: String) : AuthState()
  data class RegistrationSuccess(val message: String) : AuthState()
  data class Error(val message: String) : AuthState()
}
