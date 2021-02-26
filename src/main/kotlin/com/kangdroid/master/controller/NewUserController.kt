package com.kangdroid.master.controller

import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import com.kangdroid.master.security.JWTTokenProvider
import com.kangdroid.master.service.PasswordEncryptorService
import com.kangdroid.master.service.UserService
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class NewUserController{
    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncryptorService

    @Autowired
    private lateinit var userService: UserService

    // Just for testing with postman
    @PostMapping("/api/client/register")
    fun register(@RequestBody userRegisterDto: UserRegisterDto): UserRegisterResponseDto {
        return userService.registerUser(userRegisterDto)
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