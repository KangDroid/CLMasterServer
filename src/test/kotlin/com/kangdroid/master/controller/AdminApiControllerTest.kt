package com.kangdroid.master.controller

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.service.NodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount.manyTimes
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminApiControllerTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var nodeRepository: NodeRepository

    @Autowired
    private lateinit var testConfiguration: TestConfiguration

    @Autowired
    private lateinit var nodeService: NodeService

    private val baseUrl: String = "http://localhost"

    @After
    fun cleanDb() {
        nodeRepository.deleteAll()
    }

    @Test
    fun isRegisterNodeWorking() {
        val originalRequestFactory: ClientHttpRequestFactory = nodeService.restTemplate.requestFactory
        val mockServer: MockRestServiceServer = MockRestServiceServer.bindTo(nodeService.restTemplate).build()
        mockServer.expect(manyTimes(), requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/alive"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"isDockerServerRunning\": true, \"errorMessage\": \"\"}", MediaType.APPLICATION_JSON))

        // Let
        val url: String = "$baseUrl:$port/api/admin/node/register"
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
                id = 10,
                hostName = "testing",
                hostPort = testConfiguration.computeNodeServerPort,
                ipAddress = testConfiguration.computeNodeServerHostName
        )

        // do work
        val responseEntity: ResponseEntity<String> = testRestTemplate.postForEntity(url, nodeSaveRequestDto, NodeSaveRequestDto::class)
        val returnValue: String = responseEntity.body ?: "Error"
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

        // Reset
        nodeService.restTemplate.requestFactory = originalRequestFactory
    }
}