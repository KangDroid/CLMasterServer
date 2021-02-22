package com.kangdroid.master.controller

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeLoadResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.service.NodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClientApiControllerTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testConfiguration: TestConfiguration

    @Autowired
    private lateinit var nodeRepository: NodeRepository

    @Autowired
    private lateinit var nodeService: NodeService

    private val baseUrl: String = "http://localhost"

    @Before
    @After
    fun clearAllRepo() {
        nodeRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun isMasterAliveWorking() {
        // This is trivial test though
        val urlFinal: String = "$baseUrl:$port/api/client/alive"
        val responseEntity: ResponseEntity<Boolean> = testRestTemplate.getForEntity(urlFinal, Boolean::class)
        assertThat(responseEntity.body).isNotEqualTo(null)

        val responseBoolean: Boolean = responseEntity.body!!
        assertThat(responseBoolean).isEqualTo(true)
    }

    @Test
    fun isGettingNodeLoadWorking() {
        // URL
        val urlFinal: String = "$baseUrl:$port/api/client/node/load"
        // save node first
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )

        // Save node
        var returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Request
        val responseLoad: ResponseEntity<Array<NodeLoadResponseDto>> = testRestTemplate.getForEntity(urlFinal, Array<NodeLoadResponseDto>::class)
        assertThat(responseLoad.body).isNotEqualTo(null) // Check for null

        // Get Response Value
        val responseValue: Array<NodeLoadResponseDto> = responseLoad.body!!
        for (loadDto in responseValue) {
            assertThat(loadDto.regionName).isNotEqualTo("")
            assertThat(loadDto.nodeLoadPercentage).isNotEqualTo("")
        }
    }
}