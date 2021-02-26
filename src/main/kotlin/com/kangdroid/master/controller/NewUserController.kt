package com.kangdroid.master.controller

import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
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
    @PostMapping("/api/client/register")
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

    @PostMapping("/api/client/login")
    fun login(@RequestBody userLoginRequestDto: UserLoginRequestDto): UserLoginResponseDto {
        val user: User = userRepository.findByUserName(userLoginRequestDto.userName)
            ?: return UserLoginResponseDto(
                errorMessage = "Cannot find user: ${userLoginRequestDto.userName}"
            )

        runCatching {
            require(passwordEncoder.isMatching(userLoginRequestDto.userPassword, user.password)) { "Wrong Password" }
        }.onFailure {
            return UserLoginResponseDto(
                errorMessage = "Password is incorrect!"
            )
        }

        return UserLoginResponseDto(
            token = jwtTokenProvider.createToken(userLoginRequestDto.userName, user.roles.toList())
        )
    }
}