package com.kangdroid.master.service

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.*
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount.manyTimes
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@RunWith(SpringRunner::class)
@SpringBootTest
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testConfiguration: TestConfiguration

    @Autowired
    private lateinit var nodeService: NodeService

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

    @After
    fun clearUserDb() {
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
    fun isRegisterWorkingWell() {
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        var registerResponse: UserRegisterResponseDto = userService.registerUser(userRegisterDto)

        // First, Correct[True] Test
        assertThat(registerResponse.errorMessage).isEqualTo("")
        assertThat(registerResponse.registeredId).isEqualTo(userRegisterDto.userName)

        // Second Save, should fail.
        registerResponse = userService.registerUser(userRegisterDto)
        assertThat(registerResponse.errorMessage).isNotEqualTo("")
        assertThat(registerResponse.registeredId).isEqualTo("")
    }

    @Test
    fun isLoginWorkingWell() {
        // Register Operation
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        val registerResponse: UserRegisterResponseDto = userService.registerUser(userRegisterDto)

        // Check for ID Created well
        assertThat(registerResponse.errorMessage).isEqualTo("")

        // Trying Login
        var loginResponse: UserLoginResponseDto = userService.login(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = userRegisterDto.userPassword
            ),
            "127.0.0.1" // self loopback
        )

        // Login Assert
        assertThat(loginResponse.errorMessage).isEqualTo("")
        assertThat(loginResponse.token).isNotEqualTo("")

        // Re-Login so testing another token to be registered
        val curToken: String = loginResponse.token
        loginResponse = userService.login(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = userRegisterDto.userPassword
            ),
            "127.0.0.1" // self loopback
        )

        // Login Assert
        assertThat(loginResponse.errorMessage).isEqualTo("")
        assertThat(loginResponse.token).isNotEqualTo(curToken)

        // Wrong Login - ID
        loginResponse = userService.login(
            UserLoginRequestDto(
                userName = "ID_INCORRECT",
                userPassword = userRegisterDto.userPassword
            ),
            "127.0.0.1"
        )
        assertThat(loginResponse.errorMessage).isNotEqualTo("")
        assertThat(loginResponse.token).isEqualTo("")

        // Wrong Login - PW
        loginResponse = userService.login(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = "WrongPassword"
            ),
            "127.0.0.1"
        )
        assertThat(loginResponse.errorMessage).isNotEqualTo("")
        assertThat(loginResponse.token).isEqualTo("")
    }

    @Test
    fun isListingNodeWorksWell() {
        val loginToken = registerDemoUser()

        // Those above procedure was long, but register op.
        var responseList: List<UserImageListResponseDto> = userService.listContainer(loginToken)

        // There should be no list at all, because there is no registered container though.
        assertThat(responseList.size).isEqualTo(0)

        // Wrong Input
        responseList = userService.listContainer("")
        assertThat(responseList.size).isEqualTo(1)
        assertThat(responseList[0].errorMessage).isEqualTo("Cannot Find User. Please Re-Login")

        // With Some dummy image
        val user: User? = userRepository.findByUserToken(loginToken)
        assertThat(user).isNotEqualTo(null)
        user!!.dockerImage.add(
            DockerImage(
                dockerId = "",
                user = user,
                computeRegion = ""
            )
        )
        userRepository.save(user)

        responseList = userService.listContainer(loginToken)
        assertThat(responseList.size).isEqualTo(1)
        assertThat(responseList[0].errorMessage).isEqualTo("")
    }

    @Test
    fun isCheckingTokenWorksWell() {
        val loginToken: String = registerDemoUser()

        // CheckToken
        assertThat(
            userService.checkToken(
                loginToken
            )
        ).isEqualTo(true)

        assertThat(
            userService.checkToken(
                "loginResponse.token"
            )
        ).isEqualTo(false)
    }

    @Test
    fun isSavingWithCheckWorksWell() {
        val loginToken: String = registerDemoUser()
        val userImageResponseDto: UserImageResponseDto = UserImageResponseDto() // empty one

        // With Correct Token
        var responseString: String = userService.saveWithCheck(loginToken, userImageResponseDto)
        assertThat(responseString).isEqualTo("")

        // With Wrong Token
        responseString = userService.saveWithCheck("", userImageResponseDto)
        assertThat(responseString).isNotEqualTo("")
    }

    @Test
    fun isRestartingContainerWorksWell() {
        val loginToken: String = registerDemoUser()

        // Register Compute Node
        // Let
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )
        val returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Now on
        val userImageSaveRequestDto: UserImageSaveRequestDto = UserImageSaveRequestDto(
            userToken = loginToken,
            computeRegion = returnValue.regionName
        )

        // do work[Successful one]
        val userImageResponseDto: UserImageResponseDto = nodeService.createContainer(userImageSaveRequestDto)

        // userRestartRequestDto
        val userRestartRequestDto: UserRestartRequestDto = UserRestartRequestDto(
            userToken = loginToken,
            containerId = userImageResponseDto.containerId
        )

        // Do work
        var userRestartResponseDto: UserRestartResponseDto = userService.restartContainer(userRestartRequestDto)
        assertThat(userRestartResponseDto.errorMessage).isEqualTo("")

        // Do work[Failure: Wrong Token]
        userRestartRequestDto.userToken = ""
        userRestartResponseDto = userService.restartContainer(userRestartRequestDto)
        assertThat(userRestartResponseDto.errorMessage).isEqualTo("Cannot find user with token!")
        userRestartRequestDto.userToken = loginToken // restore token

        // Do work[Failure: Wrong ID]
        userRestartRequestDto.containerId = ""
        userRestartResponseDto = userService.restartContainer(userRestartRequestDto)
        assertThat(userRestartResponseDto.errorMessage).isEqualTo("Cannot find container ID!")
        userRestartRequestDto.containerId = userImageResponseDto.containerId // Restore Container ID

        // Do work[Failure: Internal Node Server Error]
        val originalRequestFactory: ClientHttpRequestFactory = nodeService.restTemplate.requestFactory
        val mockServer: MockRestServiceServer = MockRestServiceServer.bindTo(nodeService.restTemplate).build()
        mockServer.expect(MockRestRequestMatchers.requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/node/restart"))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andRespond(MockRestResponseCreators.withServerError()) // Internal Error
        userRestartResponseDto = userService.restartContainer(userRestartRequestDto)
        assertThat(userRestartResponseDto.errorMessage).isNotEqualTo("")
        assertThat(userRestartResponseDto.errorMessage).isEqualTo("Cannot communicate with Compute node!")
        nodeService.restTemplate.requestFactory =
            originalRequestFactory // Restore requestFactory on nodeServer's restTemplate
    }
}