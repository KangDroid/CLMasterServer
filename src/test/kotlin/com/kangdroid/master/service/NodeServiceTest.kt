package com.kangdroid.master.service

import com.kangdroid.master.config.TestConfiguration
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeLoadResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
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
    private lateinit var passwordEncryptorService: PasswordEncryptorService

    @Autowired
    private lateinit var testConfiguration: TestConfiguration

    @After
    fun clearAllRepo() {
        nodeRepository.deleteAll()
    }

    @Test
    fun isGetLoadWorksWell() {
        // Let
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
                id = 10,
                hostName = "testing",
                hostPort = "8080",
                ipAddress = "192.168.0.52"
        )
        val returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Assert
        if (returnValue.errorMessage.isEmpty()) {
            val list: List<NodeLoadResponseDto> = nodeService.getNodeLoad()
            assertThat(list.size).isEqualTo(1)
            assertThat(list[0].nodeLoadPercentage).isNotEmpty
            assertThat(list[0].regionName).isNotEmpty
//            println("Region: ${list[0].regionName}")
//            println("Current Load Percentage: ${list[0].nodeLoadPercentage}%")
        }
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
        val returnValue: NodeSaveResponseDto = nodeService.save(nodeSaveRequestDto)

        // Assert
        assertThat(returnValue.regionName.length).isGreaterThan(0)
        assertThat(returnValue.regionName).isEqualTo("Region-${nodeRepository.count() - 1}")
    }

    @Test
    fun isEncryptingPasswordWell() {
        // Let
        val plainPassword: String = "testPassword"
        val encodedPassword: String = passwordEncryptorService.encodePlainText(plainPassword)

        assertThat(passwordEncryptorService.isMatching(plainPassword, encodedPassword)).isEqualTo(true)
    }
}