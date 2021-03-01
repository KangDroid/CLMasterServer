package com.kangdroid.master.service

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.UserImageResponseDto
import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeInformationResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
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
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@RunWith(SpringRunner::class)
@SpringBootTest
class NodeServiceTest {

    @Autowired
    private lateinit var nodeService: NodeService

    @Autowired
    private lateinit var nodeRepository: NodeRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncryptorService: PasswordEncryptorService

    @Autowired
    private lateinit var testConfiguration: TestConfiguration

    // Mock Rest Service
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var clientHttpRequestFactory: ClientHttpRequestFactory // For getting real server one

    @Before
    @After
    fun clearAllRepo() {
        nodeRepository.deleteAll()
        userRepository.deleteAll()
    }

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
    fun isGetLoadWorksWell() {
        // Let
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )
        nodeService.save(nodeSaveRequestDto)

        // Assert
        val responseEntity: ResponseEntity<List<NodeInformationResponseDto>> =
            nodeService.getNodeInformation()
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotEqualTo(null)

        val list: List<NodeInformationResponseDto> = responseEntity.body!!
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0].nodeLoadPercentage).isNotEmpty
        assertThat(list[0].regionName).isNotEmpty
    }

    @Test
    fun isSavingWorksWell() {
        // Let
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )

        // do work
        var returnEntity: ResponseEntity<NodeSaveResponseDto> = nodeService.save(nodeSaveRequestDto)
        assertThat(returnEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(returnEntity.body).isNotEqualTo(null)

        var returnValue: NodeSaveResponseDto = returnEntity.body!!

        // Assert
        assertThat(returnValue.regionName.length).isGreaterThan(0)
        assertThat(returnValue.regionName).isEqualTo("Region-${nodeRepository.count() - 1}")

        // Duplication Test
        runCatching {
            returnEntity = nodeService.save(nodeSaveRequestDto)
        }.onSuccess {
            fail("Should raise duplicated - related exception, but somehow it succeed!")
        }.onFailure {
            assertThat(it.message).contains("Duplicated Compute Node is found on IP Address")
        }

        // Wrong IP Address[False]
        val originalRequestFactory: ClientHttpRequestFactory = nodeService.restTemplate.requestFactory
        val mockServerFailing: MockRestServiceServer = MockRestServiceServer.bindTo(nodeService.restTemplate).build()
        mockServerFailing.expect(requestTo("http://whatever:9090/api/alive"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError()) // Internal Error

        runCatching {
            returnEntity = nodeService.save(
                NodeSaveRequestDto(
                    id = 10,
                    hostName = "",
                    hostPort = "9090",
                    ipAddress = "whatever"
                )
            )
        }.onSuccess {
            fail("We have mocked our server to force-fail. But somehow it succeed.")
        }.onFailure {
            assertThat(it.message).isNotEqualTo("No Message")
        }
        nodeService.restTemplate.requestFactory = originalRequestFactory // restore working template
    }

    @Test
    fun isEncryptingPasswordWell() {
        // Let
        val plainPassword: String = "testPassword"
        val encodedPassword: String = passwordEncryptorService.encodePlainText(plainPassword)

        assertThat(passwordEncryptorService.isMatching(plainPassword, encodedPassword)).isEqualTo(true)
    }

    @Test
    fun isCreatingContainerWorksWell() {
        // Get Login Token
        val loginToken = registerDemoUser()

        // Register Compute Node
        // Let
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
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
        var userImageResponseEntity: ResponseEntity<UserImageResponseDto> =
            nodeService.createContainer(userImageSaveRequestDto)
        assertThat(userImageResponseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(userImageResponseEntity.body).isNotEqualTo(null)

        var userImageResponseDto: UserImageResponseDto = userImageResponseEntity.body as UserImageResponseDto
        assertThat(userImageResponseDto.containerId).isNotEqualTo("")
        assertThat(userImageResponseDto.targetIpAddress).isNotEqualTo("")
        assertThat(userImageResponseDto.targetPort).isNotEqualTo("")
        assertThat(userImageResponseDto.regionLocation).isEqualTo(returnValue.regionName)

        // do work[Failure: Wrong Compute Region]
        userImageSaveRequestDto.computeRegion = ""
        runCatching {
            userImageResponseEntity = nodeService.createContainer(userImageSaveRequestDto)
        }.onSuccess {
            fail("Seems like this should fail, but succeed somehow!")
        }.onFailure {
            assertThat(it.message).isEqualTo("Cannot find Compute Region!")
        }
        userImageSaveRequestDto.computeRegion = returnValue.regionName // restore region

        // do work[Failure: Wrong token somehow]
        userImageSaveRequestDto.userToken = ""
        runCatching {
            userImageResponseEntity = nodeService.createContainer(userImageSaveRequestDto)
        }.onSuccess {
            fail("Seems like this should fail, but succeed somehow!")
        }
        userImageSaveRequestDto.userToken = loginToken // restore token

        // setup mock
        val originalRequestFactory: ClientHttpRequestFactory = nodeService.restTemplate.requestFactory
        val mockServerFailing: MockRestServiceServer = MockRestServiceServer.bindTo(nodeService.restTemplate).build()
        mockServerFailing.expect(requestTo("http://${testConfiguration.computeNodeServerHostName}:${testConfiguration.computeNodeServerPort}/api/node/image"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError()) // Internal Error

        // do work[Failure: Compute Node Error]
        runCatching {
            userImageResponseEntity = nodeService.createContainer(userImageSaveRequestDto)
        }.onSuccess {
            fail("Seems like this should fail, but succeed somehow!")
        }.onFailure {
            assertThat(it.message).isEqualTo("Cannot communicate with Compute node!")
        }

        mockServerFailing.verify()
        nodeService.restTemplate.requestFactory = originalRequestFactory // Restore working server
    }

    @Test
    fun isRestartContainerWorksWell() {
        // Get Login Token
        val loginToken = registerDemoUser()

        // Register Compute Node
        // Let
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
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

        // Create Container
        val userImageResponseDto: UserImageResponseDto =
            nodeService.createContainer(userImageSaveRequestDto).body as UserImageResponseDto

        // Find Docker Image Entity
        val userName: String? = userService.getUserName(loginToken)
        assertThat(userName).isNotEqualTo(null) // username should not be equal

        val user: User = userRepository.findByUserName(userName!!)!!
        lateinit var dockerImage: DockerImage
        for (dockerImageTest in user.dockerImage) {
            if (dockerImageTest.dockerId == userImageResponseDto.containerId) {
                dockerImage = dockerImageTest
                break
            }
        }

        // do work[Successful work]
        var responseString: String = nodeService.restartContainer(dockerImage)
        assertThat(responseString).isEqualTo("")

        // do work[Failure: Wrong Compute Region somehow]
        dockerImage.computeRegion = ""
        responseString = nodeService.restartContainer(dockerImage)
        assertThat(responseString).isEqualTo("Cannot find Compute Region!")
    }
}