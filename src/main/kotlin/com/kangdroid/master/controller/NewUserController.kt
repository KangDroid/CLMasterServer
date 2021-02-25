package com.kangdroid.master.controller

import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import com.kangdroid.master.security.JWTTokenProvider
import com.kangdroid.master.service.PasswordEncryptorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class NewUserController(
    private val jwtTokenProvider: JWTTokenProvider,
    private val userRepository: UserRepository
) {

    @Autowired
    private lateinit var passwordEncoder: PasswordEncryptorService

    // Just for testing with postman
    @PostMapping("/join")
    fun join(@RequestBody user: Map<String, String>): UserRegisterResponseDto {
        runCatching {
            userRepository.save(User(
                email = user.get("email")!!,
                userPassword = passwordEncoder.encodePlainText(user.get("password")!!),
                roles = setOf("ROLE_USER"),
                userName = ""
            ))
        }.onSuccess {
            return UserRegisterResponseDto(
                registeredId = it.email,
                errorMessage = ""
            )
        }.onFailure {
            return UserRegisterResponseDto(errorMessage = "${it.message}")
        }

        return UserRegisterResponseDto(
            errorMessage = "Possible?"
        )
    }

    @PostMapping("/login")
    fun login(@RequestBody user: Map<String, String>): String {
        val member = userRepository.findByEmail(user["email"]!!)
            .orElseThrow {
                IllegalArgumentException(
                    "Unknown Email Address"
                )
            }
        require(passwordEncoder.isMatching(user["password"]!!, member.password)) { "Wrong Password" }
        return jwtTokenProvider.createToken(member.username!!, member.roles.toList())
    }
}