package com.kangdroid.master.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@ConfigurationProperties(prefix = "test")
class TestConfiguration {
    lateinit var computeNodeServerHostName: String
    lateinit var computeNodeServerPort: String
    lateinit var computeNodeServerAddress: String

    @PostConstruct
    fun setAddress() {
        computeNodeServerAddress = "http://$computeNodeServerHostName:$computeNodeServerPort"
    }
}