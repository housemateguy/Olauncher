package app.olauncher.helper

import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class PasswordHelper {
    companion object {
        private const val TAG = "PasswordHelper"
        
        /**
         * Hash a password using SHA-256
         */
        fun hashPassword(password: String, salt: String = ""): String {
            return try {
                val combined = password + salt
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(combined.toByteArray())
                Base64.getEncoder().encodeToString(hash)
            } catch (e: Exception) {
                Log.e(TAG, "Error hashing password: ${e.message}")
                ""
            }
        }
        
        /**
         * Generate a random salt
         */
        fun generateSalt(): String {
            val random = SecureRandom()
            val salt = ByteArray(16)
            random.nextBytes(salt)
            return Base64.getEncoder().encodeToString(salt)
        }
        
        /**
         * Verify a password against a stored hash
         */
        fun verifyPassword(password: String, storedHash: String, salt: String = ""): Boolean {
            val inputHash = hashPassword(password, salt)
            return inputHash == storedHash
        }
        
        /**
         * Check if password meets minimum requirements
         */
        fun isPasswordValid(password: String): Boolean {
            return password.length >= 4 && password.length <= 20
        }
    }
} 