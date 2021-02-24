package com.kangdroid.master.controller

import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.security.JWTTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class NewUserController(
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JWTTokenProvider,
    private val userRepository: UserRepository
) {

    // Just for testing with postman
    @PostMapping("/join")
    fun join(@RequestBody user: Map<String, String>): Long {
        return userRepository.save(User(
            email = user.get("email")!!,
            userPassword = passwordEncoder.encode(user.get("password")),
            roles = setOf("ROLE_USER"),
            userName = ""
        )).id
    }

    @PostMapping("/login")
    fun login(@RequestBody user: Map<String, String>): String {
        val member = userRepository.findByEmail(user["email"]!!)
            .orElseThrow {
                IllegalArgumentException(
                    "Unknown Email Address"
                )
            }
        require(passwordEncoder.matches(user["password"], member.password)) { "Wrong Password" }
        return jwtTokenProvider.createToken(member.username!!, member.roles.toList())
    }
}