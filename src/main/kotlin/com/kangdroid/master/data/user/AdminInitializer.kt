package com.kangdroid.master.data.user

import com.kangdroid.master.service.PasswordEncryptorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class AdminInitializer {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncryptorService

    @PostConstruct
    fun initAdmin() {
        userRepository.save(
            User(
                userName = "root",
                userPassword = passwordEncoder.encodePlainText("testPassword"),
                roles = setOf("ROLE_ADMIN")
            )
        )
    }
}