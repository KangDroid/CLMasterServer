package com.kangdroid.master.controller

import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import com.kangdroid.master.security.JWTTokenProvider
import com.kangdroid.master.service.PasswordEncryptorService
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
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
    fun join(@RequestBody userRegisterDto: UserRegisterDto): UserRegisterResponseDto {
        lateinit var userRegisterResponseDto: UserRegisterResponseDto
        runCatching {
            userRepository.save(User(
                userName = userRegisterDto.userName,
                userPassword = passwordEncoder.encodePlainText(userRegisterDto.userPassword),
                roles = setOf("ROLE_USER")
            ))
        }.onSuccess {
            userRegisterResponseDto = UserRegisterResponseDto(
                registeredId = it.userName,
                errorMessage = ""
            )
        }.onFailure {
            userRegisterResponseDto = if (it.cause is ConstraintViolationException) {
                UserRegisterResponseDto(errorMessage = "E-Mail address is already registered!")
            } else {
                UserRegisterResponseDto(errorMessage = "Unknown Throw: ${it.cause.toString()}")
            }
        }

        return userRegisterResponseDto
    }

    @PostMapping("/login")
    fun login(@RequestBody user: Map<String, String>): String {
        val member = userRepository.findByUserName(user["email"]!!)
            ?: throw IllegalArgumentException(
                "Unknown Email Address"
            )
        require(passwordEncoder.isMatching(user["password"]!!, member.password)) { "Wrong Password" }
        return jwtTokenProvider.createToken(member.username!!, member.roles.toList())
    }
}