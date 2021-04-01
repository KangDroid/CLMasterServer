package com.kangdroid.master

import com.kangdroid.master.config.AdminNodeConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AdminNodeConfig::class)
class CLMasterServer

fun main(args: Array<String>) {
    runApplication<CLMasterServer>(*args)
}
