package com.kangdroid.master.data.user

import com.kangdroid.master.service.PasswordEncryptorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class AdminInitializer {

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncryptorService

    @PostConstruct
    fun initAdmin() {
        userTemplateRepository.saveUser(
            User(
                userName = "root",
                userPassword = passwordEncoder.encodePlainText("testPassword"),
                roles = setOf("ROLE_ADMIN")
            )
        )
    }
}