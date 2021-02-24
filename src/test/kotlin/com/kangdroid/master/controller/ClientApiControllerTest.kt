package com.kangdroid.master.controller

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.docker.dto.*
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeInformationResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import com.kangdroid.master.service.NodeService
import com.kangdroid.master.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount.manyTimes
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

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

    @Autowired
    private lateinit var userService: UserService

    private val baseUrl: String = "http://localhost"

    // Mock Rest Service
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var clientHttpRequestFactory: ClientHttpRequestFactory // For getting real server one

    @PostConstruct
    fun initMockServer() {
        clientHttpRequestFactory = nodeService.restTemplate.requestFactory
        mockServer = MockRestServiceServer.bindTo(nodeService.restTemplate)
            .ignoreExpectOrder(true).build()
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

        mockServer.expect(
            manyTimes(),
            requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/node/load")
        )
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("1.05", MediaType.TEXT_PLAIN))

        mockServer.expect(
            manyTimes(),
            requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/node/port")
        )
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("1234", MediaType.TEXT_PLAIN))

        mockServer.expect(
            manyTimes(),
            requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/node/image")
        )
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    "{\"targetIpAddress\": \"127.0.0.1\", \"targetPort\":\"1234\", \"containerId\":\"1234test\", \"regionLocation\":\"Region-0\", \"errorMessage\":\"\"}",
                    MediaType.APPLICATION_JSON
                )
            )

        mockServer.expect(
            manyTimes(),
            requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/node/restart")
        )
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("", MediaType.TEXT_PLAIN))
    }

    @PreDestroy
    fun destroyMockServer() {
        nodeService.restTemplate.requestFactory = clientHttpRequestFactory
    }

    @Before
    @After
    fun clearAllRepo() {
        nodeRepository.deleteAll()
        userRepository.deleteAll()
    }

    // Register Demo User for testing purpose.
    // This should not assert!
    fun registerDemoUser(): String {
        // Register Operation
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        val registerResponse: UserRegisterResponseDto = userService.registerUser(userRegisterDto)

        // Trying Login
        val loginResponse: UserLoginResponseDto = userService.login(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = userRegisterDto.userPassword
            ),
            "127.0.0.1" // self loopback
        )

        return loginResponse.token
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
        val urlFinal: String = "$baseUrl:$port/api/client/node"
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
        val responseInformation: ResponseEntity<Array<NodeInformationResponseDto>> =
            testRestTemplate.getForEntity(urlFinal, Array<NodeInformationResponseDto>::class)
        assertThat(responseInformation.body).isNotEqualTo(null) // Check for null

        // Get Response Value
        val responseValue: Array<NodeInformationResponseDto> = responseInformation.body!!
        for (loadDto in responseValue) {
            assertThat(loadDto.regionName).isNotEqualTo("")
            assertThat(loadDto.nodeLoadPercentage).isNotEqualTo("")
        }
    }

    @Test
    fun isCreatingContainerRequestWorking() {
        // Login Token
        val loginToken = registerDemoUser()

        // URL
        val urlFinal: String = "$baseUrl:$port/api/client/container"

        // save node first
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )

        // Save node
        val returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Request Dto
        val userImageSaveRequestDto: UserImageSaveRequestDto = UserImageSaveRequestDto(
            dockerId = "",
            userToken = loginToken,
            computeRegion = returnValue.regionName
        )

        // do work[Successful Operation]
        var responseEntity: ResponseEntity<UserImageResponseDto> =
            testRestTemplate.postForEntity(urlFinal, userImageSaveRequestDto, UserImageResponseDto::class)
        assertThat(responseEntity.body).isNotEqualTo(null)
        var responseValue: UserImageResponseDto = responseEntity.body!!
        assertThat((responseValue.errorMessage)).isEqualTo("")
        assertThat((responseValue.targetIpAddress)).isNotEqualTo("")
        assertThat((responseValue.targetPort)).isNotEqualTo("")
        assertThat((responseValue.containerId)).isNotEqualTo("")
        assertThat((responseValue.regionLocation)).isNotEqualTo("")

        // do work[Failure: Wrong Token]
        userImageSaveRequestDto.userToken = ""
        responseEntity = testRestTemplate.postForEntity(urlFinal, userImageSaveRequestDto, UserImageResponseDto::class)
        assertThat(responseEntity.body).isNotEqualTo(null)

        responseValue = responseEntity.body!!
        assertThat(responseValue.errorMessage).isEqualTo("Token is Invalid. Please Re-Login")
    }

    @Test
    fun isRegisteringUserWorksWell() {
        // Let
        val finalUrl: String = "$baseUrl:$port/api/client"
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "testing",
            userPassword = "testing_password"
        )

        // do work
        val responseEntity: ResponseEntity<UserRegisterResponseDto> =
            testRestTemplate.postForEntity(finalUrl, userRegisterDto, UserRegisterResponseDto::class)
        assertThat(responseEntity.body).isNotEqualTo(null)

        val responseValue: UserRegisterResponseDto = responseEntity.body!!
        assertThat(responseValue.errorMessage).isEqualTo("")
        assertThat(responseValue.registeredId).isEqualTo(userRegisterDto.userName)
    }

    @Test
    fun isLoggingInWorksWell() {
        // Register First
        val registerUrl: String = "$baseUrl:$port/api/client"
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "testing",
            userPassword = "testing_password"
        )

        // do work
        val registerResponseEntity: ResponseEntity<UserRegisterResponseDto> =
            testRestTemplate.postForEntity(registerUrl, userRegisterDto, UserRegisterResponseDto::class)

        // Let
        val finalUrl: String = "$baseUrl:$port/api/client/login"
        val userLoginRequestDto: UserLoginRequestDto = UserLoginRequestDto(
            userName = userRegisterDto.userName,
            userPassword = userRegisterDto.userPassword
        )

        // Do Post
        var responseEntity: ResponseEntity<UserLoginResponseDto> =
            testRestTemplate.postForEntity(finalUrl, userLoginRequestDto, UserLoginResponseDto::class)
        assertThat(responseEntity.body).isNotEqualTo(null)
        var responseValue: UserLoginResponseDto = responseEntity.body!!
        assertThat(responseValue.errorMessage).isEqualTo("")
        assertThat(responseValue.token).isNotEqualTo("")

        // Without X-FORWARDED-FOR
        val headers: HttpHeaders = HttpHeaders()
        headers.set("X-FORWARDED-FOR", null)

        // Set Entity
        val entity: HttpEntity<UserLoginRequestDto> = HttpEntity<UserLoginRequestDto>(userLoginRequestDto, headers)

        // Request[Successful one]
        responseEntity = testRestTemplate.exchange(finalUrl, HttpMethod.POST, entity, UserLoginResponseDto::class)
        assertThat(responseEntity.body).isNotEqualTo(null)
        responseValue = responseEntity.body!!
        assertThat(responseValue.errorMessage).isEqualTo("")
        assertThat(responseValue.token).isNotEqualTo("")
    }

    @Test
    fun isRestartingContainerNodeWorksWell() {
        val loginToken = registerDemoUser()
        // URL
        val urlFinal: String = "$baseUrl:$port/api/client/node/create"
        val restartUrl: String = "$baseUrl:$port/api/client/restart"

        // save node first
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )

        // Save node
        val returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Request Dto
        val userImageSaveRequestDto: UserImageSaveRequestDto = UserImageSaveRequestDto(
            dockerId = "",
            userToken = loginToken,
            computeRegion = returnValue.regionName
        )

        // Create Container
        val userImageResponseDto: UserImageResponseDto = nodeService.createContainer(userImageSaveRequestDto)

        // Restart Request Dto
        val userRestartRequestDto: UserRestartRequestDto = UserRestartRequestDto(
            userToken = loginToken,
            containerId = userImageResponseDto.containerId
        )

        // Request
        var responseEntity: ResponseEntity<UserRestartResponseDto> =
            testRestTemplate.postForEntity(restartUrl, userRestartRequestDto, UserRestartResponseDto::class)
        assertThat(responseEntity.body).isNotEqualTo(null)
        var responseValue: UserRestartResponseDto = responseEntity.body!!
        assertThat(responseValue.errorMessage).isEqualTo("")

        // Request[Failure: Wrong Token]
        userRestartRequestDto.userToken = ""
        responseEntity =
            testRestTemplate.postForEntity(restartUrl, userRestartRequestDto, UserRestartResponseDto::class)
        assertThat(responseEntity.body).isNotEqualTo(null)
        responseValue = responseEntity.body!!
        assertThat(responseValue.errorMessage).isEqualTo("Token is Invalid. Please Re-Login")
    }

    @Test
    fun isContainerListingWorksWell() {
        val finalUrl: String = "$baseUrl:$port/api/client/container"
        val loginToken: String = registerDemoUser()

        // Set HTTP Headers
        val headers: HttpHeaders = HttpHeaders()
        headers.set("userToken", loginToken)

        // Set Entity
        val entity: HttpEntity<String> = HttpEntity<String>(headers)

        // Request[Successful one]
        var responseEntity: ResponseEntity<Array<UserImageListResponseDto>> =
            testRestTemplate.exchange(finalUrl, HttpMethod.GET, entity, Array<UserImageListResponseDto>::class)
        assertThat(responseEntity.body).isNotEqualTo(null)

        var responseValue: Array<UserImageListResponseDto> = responseEntity.body!!
        assertThat(responseValue.size).isEqualTo(0)

        // Request[Failure: Wrong Token]
        headers.set("userToken", "a")
        responseEntity =
            testRestTemplate.exchange(finalUrl, HttpMethod.GET, entity, Array<UserImageListResponseDto>::class)
        assertThat(responseEntity.body).isNotEqualTo(null)

        responseValue = responseEntity.body!!
        assertThat(responseValue.size).isEqualTo(1)
        assertThat(responseValue[0].errorMessage).isNotEqualTo("")
        assertThat(responseValue[0].errorMessage).isEqualTo("Token is Invalid. Please Re-Login")
    }
}