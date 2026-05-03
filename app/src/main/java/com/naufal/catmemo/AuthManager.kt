package com.naufal.catmemo

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("CatMemoAuth", Context.MODE_PRIVATE)

    fun register(username: String, password: String): Boolean {
        if (prefs.contains(username)) return false // Username already exists
        prefs.edit().putString(username, password).apply()
        return true
    }

    fun login(username: String, password: String): Boolean {
        val savedPassword = prefs.getString(username, null)
        if (savedPassword != null && savedPassword == password) {
            prefs.edit().putString("LOGGED_IN_USER", username).apply()
            return true
        }
        return false
    }

    fun logout() {
        prefs.edit().remove("LOGGED_IN_USER").apply()
    }

    fun getLoggedInUser(): String? {
        return prefs.getString("LOGGED_IN_USER", null)
    }

    fun isLoggedIn(): Boolean {
        return getLoggedInUser() != null
    }

    // Profile Management
    fun getUserProfile(username: String): UserProfile {
        val nickname = prefs.getString("${username}_nickname", "") ?: ""
        val dob = prefs.getString("${username}_dob", "") ?: ""
        val photoUri = prefs.getString("${username}_photoUri", "") ?: ""
        val language = prefs.getString("${username}_language", "in") ?: "in"
        return UserProfile(nickname, dob, photoUri, language)
    }

    fun saveUserProfile(username: String, profile: UserProfile) {
        prefs.edit().apply {
            putString("${username}_nickname", profile.nickname)
            putString("${username}_dob", profile.dob)
            putString("${username}_photoUri", profile.photoUri)
            putString("${username}_language", profile.language)
            apply()
        }
    }

    fun changeUsername(oldUsername: String, newUsername: String): Boolean {
        if (oldUsername == newUsername) return true
        if (prefs.contains(newUsername)) return false // Username already exists

        val password = prefs.getString(oldUsername, null)
        val nickname = prefs.getString("${oldUsername}_nickname", "")
        val dob = prefs.getString("${oldUsername}_dob", "")
        val photoUri = prefs.getString("${oldUsername}_photoUri", "")
        val language = prefs.getString("${oldUsername}_language", "in")
        val notes = prefs.getString("${oldUsername}_notes", null)

        prefs.edit().apply {
            putString(newUsername, password)
            putString("${newUsername}_nickname", nickname)
            putString("${newUsername}_dob", dob)
            putString("${newUsername}_photoUri", photoUri)
            putString("${newUsername}_language", language)
            putString("${newUsername}_notes", notes)

            remove(oldUsername)
            remove("${oldUsername}_nickname")
            remove("${oldUsername}_dob")
            remove("${oldUsername}_photoUri")
            remove("${oldUsername}_language")
            remove("${oldUsername}_notes")

            putString("LOGGED_IN_USER", newUsername)
            apply()
        }
        return true
    }

    // Note Management
    private val gson = Gson()

    fun saveNotes(username: String, notes: List<Note>) {
        val json = gson.toJson(notes)
        prefs.edit().putString("${username}_notes", json).apply()
    }

    fun getNotes(username: String): List<Note> {
        val json = prefs.getString("${username}_notes", null) ?: return emptyList()
        val type = object : TypeToken<List<Note>>() {}.type
        val rawNotes: List<Note> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
        
        // Sanitization: Ensure no nulls are passed to non-nullable fields from old JSON
        return rawNotes.map { note ->
            Note(
                id = note.id ?: UUID.randomUUID().toString(),
                title = note.title ?: "",
                content = note.content ?: "",
                timestamp = if (note.timestamp == 0L) System.currentTimeMillis() else note.timestamp,
                isPinned = note.isPinned,
                textAlign = note.textAlign ?: "LEFT"
            )
        }
    }
}

data class UserProfile(
    val nickname: String = "",
    val dob: String = "",
    val photoUri: String = "",
    val language: String = "in"
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val textAlign: String = "LEFT"
)
