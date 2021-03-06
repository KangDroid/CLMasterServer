package com.kangdroid.master.controller

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.user.AdminInitializer
import com.kangdroid.master.data.user.UserTemplateRepository
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.service.NodeService
import com.kangdroid.master.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount.manyTimes
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminApiControllerTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var nodeTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var testConfiguration: TestConfiguration

    @Autowired
    private lateinit var nodeService: NodeService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var adminInitializer: AdminInitializer

    private val baseUrl: String = "http://localhost"

    @Before
    fun initSetup() {
        nodeTemplateRepository.clearAll()
        userTemplateRepository.clearAll()
        adminInitializer.initAdmin()
    }

    @After
    fun cleanDb() {
        nodeTemplateRepository.clearAll()
        userTemplateRepository.clearAll()
    }

    // Register Demo User for testing purpose.
    // This should not assert!
    fun registerDemoUser(): String {
        // Register Operation
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "root",
            userPassword = "testPassword"
        )

        // Trying Login
        val loginResponse: UserLoginResponseDto = userService.loginUser(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = userRegisterDto.userPassword
            ),
        ).body as UserLoginResponseDto

        return loginResponse.token
    }

    @Test
    fun isRegisterNodeWorking() {
        val originalRequestFactory: ClientHttpRequestFactory = nodeService.restTemplate.requestFactory
        val mockServer: MockRestServiceServer = MockRestServiceServer.bindTo(nodeService.restTemplate).build()
        mockServer.expect(
            manyTimes(),
            requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/alive")
        )
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    "{\"isDockerServerRunning\": true, \"errorMessage\": \"\"}",
                    MediaType.APPLICATION_JSON
                )
            )

        // Let
        val loginToken: String = registerDemoUser()
        val url: String = "$baseUrl:$port/api/admin/node/register"
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = ObjectId(),
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )

        // do work
        val httpHeaders: HttpHeaders = HttpHeaders().apply {
            add("X-AUTH-TOKEN", loginToken)
        }
        val responseEntity: ResponseEntity<String> =
            testRestTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity<NodeSaveRequestDto>(nodeSaveRequestDto, httpHeaders),
                NodeSaveRequestDto::class
            )
        val returnValue: String = responseEntity.body ?: "Error"
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

        // Reset
        nodeService.restTemplate.requestFactory = originalRequestFactory
    }

    @Test
    fun isWrongNodeNotSaving() {
        // Let
        val loginToken: String = registerDemoUser()
        val url: String = "$baseUrl:$port/api/admin/node/register"
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = ObjectId(),
            hostName = "testing",
            hostPort = "9090",
            ipAddress = "whatever"
        )

        // do work
        val httpHeaders: HttpHeaders = HttpHeaders().apply {
            add("X-AUTH-TOKEN", loginToken)
        }
        val responseEntity: ResponseEntity<String> =
            testRestTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity<NodeSaveRequestDto>(nodeSaveRequestDto, httpHeaders)
            )
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}