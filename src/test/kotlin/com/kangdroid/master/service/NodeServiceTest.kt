package com.kangdroid.master.service

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.docker.dto.UserImageResponseDto
import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeLoadResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
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
import org.springframework.test.context.junit4.SpringRunner

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
    fun isGetLoadWorksWell() {
        // Let
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
            id = 10,
            hostName = "testing",
            hostPort = testConfiguration.computeNodeServerPort,
            ipAddress = testConfiguration.computeNodeServerHostName
        )
        val returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Assert
        val list: List<NodeLoadResponseDto> = nodeService.getNodeLoad()
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
        var returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Assert
        assertThat(returnValue.regionName.length).isGreaterThan(0)
        assertThat(returnValue.regionName).isEqualTo("Region-${nodeRepository.count() - 1}")

        // Duplication Test
        returnValue = nodeService.save(nodeSaveRequestDto)
        assertThat(returnValue.errorMessage).isNotEqualTo("")

        // Wrong IP Address[False]
        returnValue = nodeService.save(NodeSaveRequestDto(
            id = 10,
            hostName = "",
            hostPort = "9090",
            ipAddress = "whatever"
        ))
        assertThat(returnValue.errorMessage).isNotEqualTo("")
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
        val returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Now on
        val userImageSaveRequestDto: UserImageSaveRequestDto = UserImageSaveRequestDto(
            userToken = loginToken,
            computeRegion = returnValue.regionName
        )

        // do work[Successful one]
        var userImageResponseDto: UserImageResponseDto = nodeService.createContainer(userImageSaveRequestDto)
        assertThat(userImageResponseDto.errorMessage).isEqualTo("")
        assertThat(userImageResponseDto.containerId).isNotEqualTo("")
        assertThat(userImageResponseDto.targetIpAddress).isNotEqualTo("")
        assertThat(userImageResponseDto.targetPort).isNotEqualTo("")
        assertThat(userImageResponseDto.regionLocation).isEqualTo(returnValue.regionName)

        // do work[Failure: Wrong Compute Region]
        userImageSaveRequestDto.computeRegion = ""
        userImageResponseDto = nodeService.createContainer(userImageSaveRequestDto)
        assertThat(userImageResponseDto.errorMessage).isEqualTo("Cannot find Compute Region!")
        userImageSaveRequestDto.computeRegion = returnValue.regionName // restore region

        // do work[Failure: Wrong token somehow]
        userImageSaveRequestDto.userToken = ""
        userImageResponseDto = nodeService.createContainer(userImageSaveRequestDto)
        assertThat(userImageResponseDto.errorMessage).isEqualTo("Cannot Find User. Please Re-Login")
        userImageSaveRequestDto.userToken = loginToken // restore token
    }
}