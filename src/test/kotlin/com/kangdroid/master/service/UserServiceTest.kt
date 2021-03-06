package com.kangdroid.master.service

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.UserImageListResponseDto
import com.kangdroid.master.data.docker.dto.UserImageResponseDto
import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.docker.dto.UserRestartRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserTemplateRepository
import com.kangdroid.master.data.user.dto.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.bson.types.ObjectId
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@RunWith(SpringRunner::class)
@SpringBootTest
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

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
        userTemplateRepository.clearAll()
    }

    // Register Demo User for testing purpose.
    // This should not assert!
    fun registerDemoUser(): String {
        // Register Operation
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        val registerResponse: UserRegisterResponseDto =
            userService.registerUser(userRegisterDto).body as UserRegisterResponseDto

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
    fun isRegisterWorkingWell() {
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        var registerResponseEntity: ResponseEntity<UserRegisterResponseDto> = userService.registerUser(userRegisterDto)
        assertThat(registerResponseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(registerResponseEntity.body).isNotEqualTo(null)

        var registerResponse: UserRegisterResponseDto =
            registerResponseEntity.body as UserRegisterResponseDto

        // First, Correct[True] Test
        assertThat(registerResponse.registeredId).isEqualTo(userRegisterDto.userName)

        // Do work[Failure: Username Exists!]
        runCatching {
            registerResponseEntity = userService.registerUser(userRegisterDto)
        }.onSuccess {
            fail("This test should be failed, but it somehow succeed!")
        }.onFailure {
            assertThat(it.message).isEqualTo("E-Mail address is already registered!")
        }
    }

    @Test
    fun isLoginWorkingWell() {
        // Register Operation
        val userRegisterDto: UserRegisterDto = UserRegisterDto(
            userName = "KangDroid",
            userPassword = "TestingPassword"
        )
        val registerResponse: UserRegisterResponseDto =
            userService.registerUser(userRegisterDto).body as UserRegisterResponseDto

        // Trying Login
        var loginResponseEntity: ResponseEntity<UserLoginResponseDto> = userService.loginUser(
            UserLoginRequestDto(
                userName = userRegisterDto.userName,
                userPassword = userRegisterDto.userPassword
            )
        )
        assertThat(loginResponseEntity.statusCode).isEqualTo(HttpStatus.OK)

        var loginResponse: UserLoginResponseDto = loginResponseEntity.body!!
        // Login Assert
        assertThat(loginResponse.token).isNotEqualTo("")

        // Wrong Login - ID
        runCatching {
            loginResponseEntity = userService.loginUser(
                UserLoginRequestDto(
                    userName = "ID_INCORRECT",
                    userPassword = userRegisterDto.userPassword
                )
            )
        }.onSuccess {
            fail("This test should be failed, but it somehow succeed!")
        }.onFailure {
            assertThat(it.message).isEqualTo("Cannot find user: ID_INCORRECT")
        }

        // Wrong Login - PW
        runCatching {
            loginResponseEntity = userService.loginUser(
                UserLoginRequestDto(
                    userName = userRegisterDto.userName,
                    userPassword = "WrongPassword"
                )
            )
        }.onSuccess {
            fail("This test should be failed, but it somehow succeed!")
        }.onFailure {
            assertThat(it.message).isEqualTo("Password is incorrect!")
        }
    }

    @Test
    fun isListingNodeWorksWell() {
        val loginToken = registerDemoUser()

        // Those above procedure was long, but register op.
        var responseEntity: ResponseEntity<List<UserImageListResponseDto>> =
            userService.listContainer(loginToken)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotEqualTo(null)

        var responseList: List<UserImageListResponseDto> = responseEntity.body!!

        // There should be no list at all, because there is no registered container though.
        assertThat(responseList.size).isEqualTo(0)

        // Wrong Input
        runCatching {
            userService.listContainer("a")
        }.onSuccess {
            fail("This should be failed, but somehow it succeed!")
        }.onFailure {
            assertThat(it.message).isEqualTo("Cannot Find User. Please Re-Login")
        }

        // With Some dummy image
        val userName: String? = userService.getUserName(loginToken)
        assertThat(userName).isNotEqualTo(null) // userName should not be equal
        val user: User = userTemplateRepository.findByUserName(userName!!)
        assertThat(user).isNotEqualTo(null)
        user.dockerImage.add(
            DockerImage(
                dockerId = "",
                computeRegion = ""
            )
        )
        userTemplateRepository.saveUser(user)

        responseEntity = userService.listContainer(loginToken)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotEqualTo(null)

        responseList = responseEntity.body!!
        assertThat(responseList.size).isEqualTo(1)
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
            id = ObjectId(),
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )
        val returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto).body!!

        // Now on
        val userImageSaveRequestDto: UserImageSaveRequestDto = UserImageSaveRequestDto(
            userToken = loginToken,
            computeRegion = returnValue.regionName
        )

        // do work[Successful one]
        val userImageResponseDto: UserImageResponseDto =
            nodeService.createContainer(userImageSaveRequestDto).body as UserImageResponseDto

        // userRestartRequestDto
        val userRestartRequestDto: UserRestartRequestDto = UserRestartRequestDto(
            userToken = loginToken,
            containerId = userImageResponseDto.containerId
        )

        // Do work
        var userRestartEntity: ResponseEntity<Void> = userService.restartContainer(userRestartRequestDto)
        assertThat(userRestartEntity.statusCode).isEqualTo(HttpStatus.OK)

        // Do work[Failure: Wrong Token]
        userRestartRequestDto.userToken = ""
        runCatching {
            userRestartEntity = userService.restartContainer(userRestartRequestDto)
        }.onSuccess {
            fail("Token is null, but it succeed somehow. Aborting!")
        }.onFailure {
            assertThat(it.message).isEqualTo("Cannot find user with token!")
        }
        userRestartRequestDto.userToken = loginToken // restore token

        // Do work[Failure: Wrong ID]
        userRestartRequestDto.containerId = ""
        runCatching {
            userRestartEntity = userService.restartContainer(userRestartRequestDto)
        }.onSuccess {
            fail("Container ID is null, but it responded with - restart was succeed.")
        }.onFailure {
            assertThat(it.message).contains("Cannot find docker image ")
        }
        userRestartRequestDto.containerId = userImageResponseDto.containerId // Restore Container ID

        // Do work[Failure: Internal Node Server Error]
        val originalRequestFactory: ClientHttpRequestFactory = nodeService.restTemplate.requestFactory
        val mockServer: MockRestServiceServer = MockRestServiceServer.bindTo(nodeService.restTemplate).build()
        mockServer.expect(MockRestRequestMatchers.requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/node/restart"))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andRespond(MockRestResponseCreators.withServerError()) // Internal Error
        runCatching {
            userRestartEntity = userService.restartContainer(userRestartRequestDto)
        }.onSuccess {
            fail("Internal Node Error is expected, but it restarted node without node server somehow")
        }.onFailure {
            assertThat(it.message).isEqualTo("Cannot communicate with Compute node!")
        }
        nodeService.restTemplate.requestFactory =
            originalRequestFactory // Restore requestFactory on nodeServer's restTemplate
    }

    @Test
    fun is_getUserInformation_returning_NotFound_invalid_token() {
        runCatching {
            userService.getUserInformation("test")
        }.onSuccess {
            fail("Wrong token is passed, but somehow test succeed!")
        }.onFailure {
            assertThat(it.message).isEqualTo("Cannot find user!")
        }
    }

    @Test
    fun is_getUserInformation_returning_ok() {
        val loginToken: String = registerDemoUser()
        val requiredUserInformationResponse: UserInformationResponseDto =
            UserInformationResponseDto(
                userName = "KangDroid",
                userRole = setOf("ROLE_USER")
            )

        val responseResponse: ResponseEntity<UserInformationResponseDto>? = runCatching {
            userService.getUserInformation(loginToken)
        }.onFailure {
            fail("This test should be passed with correct token, but it failed!")
        }.getOrNull()

        assertThat(responseResponse).isNotEqualTo(null)
        assertThat(responseResponse!!.hasBody()).isEqualTo(true)
        assertThat(responseResponse.body!!.userName).isEqualTo(requiredUserInformationResponse.userName)
        assertThat(responseResponse.body!!.userRole).isEqualTo(requiredUserInformationResponse.userRole)
    }
}