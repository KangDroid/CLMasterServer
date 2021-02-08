package com.kangdroid.master.controller

import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminApiControllerTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var nodeRepository: NodeRepository

    private val baseUrl: String = "http://localhost"

    @After
    fun cleanDb() {
        nodeRepository.deleteAll()
    }

    @Test
    fun isRegisterNodeWorking() {
        // Let
        val url: String = "$baseUrl:$port/api/admin/node/register"
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
                id = 10,
                hostName = "testing",
                hostPort = "8080",
                ipAddress = "192.168.0.52"
        )

        // do work
        val responseEntity: ResponseEntity<String> = testRestTemplate.postForEntity(url, nodeSaveRequestDto, NodeSaveRequestDto::class)
        val returnValue: String = responseEntity.body ?: "Error"

        // Assert
        if (returnValue != "Error") {
            assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        }
    }
}