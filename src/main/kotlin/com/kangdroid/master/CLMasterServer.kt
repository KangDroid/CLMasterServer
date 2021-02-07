package com.kangdroid.master

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CLMasterServer

fun main(args: Array<String>) {
	runApplication<CLMasterServer>(*args)
}
