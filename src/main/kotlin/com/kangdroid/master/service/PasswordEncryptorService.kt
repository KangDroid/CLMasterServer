package com.kangdroid.master.service

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service

@Service
class PasswordEncryptorService {
    private val argon2PasswordEncoder: Argon2PasswordEncoder = Argon2PasswordEncoder(
        16, 32, 1, 4096, 5
    )

    fun encodePlainText(plainInput: String): String {
        return argon2PasswordEncoder.encode(plainInput)
    }

    fun isMatching(plainInput: String, encodedPassword: String): Boolean {
        return argon2PasswordEncoder.matches(plainInput, encodedPassword)
    }
}