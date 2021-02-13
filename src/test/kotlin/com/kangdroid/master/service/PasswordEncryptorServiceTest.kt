package com.kangdroid.master.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class PasswordEncryptorServiceTest {

    @Autowired
    private lateinit var passwordEncryptorService: PasswordEncryptorService

    @Test
    fun isEncodingDecodingWorksWell() {
        val plainPassword: String = "TestPassword"
        val encodedPassword: String = passwordEncryptorService.encodePlainText(plainPassword)

        // Assert
        assertThat(encodedPassword.length).isGreaterThan(0)
        assertThat(passwordEncryptorService.isMatching(plainPassword, encodedPassword))
            .isEqualTo(true)
        assertThat(passwordEncryptorService.isMatching("Wrong", encodedPassword))
            .isEqualTo(false)
    }
}