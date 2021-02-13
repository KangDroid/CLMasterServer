package com.kangdroid.master.service

import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @After
    fun clearUserDb() {
        userRepository.deleteAll()
    }

    @Test
    fun isRegisterWorkingWell() {
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        var registerResponse: UserRegisterResponseDto = userService.registerUser(userRegisterDto)

        // First, Correct[True] Test
        assertThat(registerResponse.errorMessage).isEqualTo("")
        assertThat(registerResponse.registeredId).isEqualTo(userRegisterDto.userName)

        // Second Save, should fail.
        registerResponse = userService.registerUser(userRegisterDto)
        assertThat(registerResponse.errorMessage).isNotEqualTo("")
        assertThat(registerResponse.registeredId).isEqualTo("")
    }

    @Test
    fun isLoginWorkingWell() {
        // Register Operation
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        val registerResponse: UserRegisterResponseDto = userService.registerUser(userRegisterDto)

        // Check for ID Created well
        assertThat(registerResponse.errorMessage).isEqualTo("")

        // Trying Login
        var loginResponse: UserLoginResponseDto = userService.login(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = userRegisterDto.userPassword
            ),
            "127.0.0.1" // self loopback
        )

        // Login Assert
        assertThat(loginResponse.errorMessage).isEqualTo("")
        assertThat(loginResponse.token).isNotEqualTo("")

        // Wrong Login - ID
        loginResponse = userService.login(
            UserLoginRequestDto(
                userName = "ID_INCORRECT",
                userPassword = userRegisterDto.userPassword
            ),
            "127.0.0.1"
        )
        assertThat(loginResponse.errorMessage).isNotEqualTo("")
        assertThat(loginResponse.token).isEqualTo("")

        // Wrong Login - PW
        loginResponse = userService.login(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = "WrongPassword"
            ),
            "127.0.0.1"
        )
        assertThat(loginResponse.errorMessage).isNotEqualTo("")
        assertThat(loginResponse.token).isEqualTo("")
    }

    @Test
    fun isCheckingTokenWorksWell() {
        // Register Operation
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        val registerResponse: UserRegisterResponseDto = userService.registerUser(userRegisterDto)

        // Check for ID Created well
        assertThat(registerResponse.errorMessage).isEqualTo("")

        // Trying Login
        val loginResponse: UserLoginResponseDto = userService.login(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = userRegisterDto.userPassword
            ),
            "127.0.0.1" // self loopback
        )

        // Login Assert
        assertThat(loginResponse.errorMessage).isEqualTo("")
        assertThat(loginResponse.token).isNotEqualTo("")

        // CheckToken
        assertThat(userService.checkToken(
            UserImageSaveRequestDto(
                loginResponse.token, "", ""
            )
        )).isEqualTo(true)

        assertThat(userService.checkToken(
            UserImageSaveRequestDto(
                "loginResponse.token", "", ""
            )
        )).isEqualTo(false)
    }
}