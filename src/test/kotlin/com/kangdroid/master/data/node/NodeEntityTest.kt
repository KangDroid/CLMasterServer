package com.kangdroid.master.data.node

import com.kangdroid.master.error.exception.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.bson.types.ObjectId
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class NodeEntityTest {

    @Autowired
    private lateinit var nodeTemplateRepository: NodeTemplateRepository

    @After
    fun clearAll() {
        nodeTemplateRepository.clearAll()
    }

    @Test
    fun is_saveNode_works_well() {
        val firstCount: Long = nodeTemplateRepository.count()
        // Let
        val nodeTmp: Node = Node(
            id = ObjectId(),
            hostName = "localhost",
            ipAddress = "192.168.0.8",
            hostPort = "8080",
            regionName = ""
        )

        // save
        val saveResponse: Node = nodeTemplateRepository.saveNode(nodeTmp)
        assertThat(saveResponse.ipAddress).isEqualTo(nodeTmp.ipAddress)
        assertThat(saveResponse.hostName).isEqualTo(nodeTmp.hostName)
        assertThat(saveResponse.hostPort).isEqualTo(nodeTmp.hostPort)
        assertThat(nodeTemplateRepository.count()).isEqualTo(firstCount+1)
    }

    @Test
    fun is_findNodeByIpAddress_throws_404_non_val() {
        runCatching {
            nodeTemplateRepository.findNodeByIpAddress("non-ex")
        }.onSuccess {
            fail("There is no such saved db. Should have been failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_findNodeByIpAddress_ok() {
        val nodeTmp: Node = Node(
            id = ObjectId(),
            hostName = "localhost",
            ipAddress = "192.168.0.8",
            hostPort = "8080",
            regionName = ""
        )
        nodeTemplateRepository.saveNode(nodeTmp)

        val saveResponse: Node = nodeTemplateRepository.findNodeByIpAddress("192.168.0.8")

        assertThat(saveResponse.ipAddress).isEqualTo(nodeTmp.ipAddress)
        assertThat(saveResponse.hostName).isEqualTo(nodeTmp.hostName)
        assertThat(saveResponse.hostPort).isEqualTo(nodeTmp.hostPort)
    }

    @Test
    fun is_findNodeByRegionName_throws_404_non_val() {
        runCatching {
            nodeTemplateRepository.findNodeByRegionName("asdf")
        }.onSuccess {
            fail("There is no such saved db. Should have been failed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_findNodeByRegionName_ok() {
        val nodeTmp: Node = Node(
            id = ObjectId(),
            hostName = "localhost",
            ipAddress = "192.168.0.8",
            hostPort = "8080",
            regionName = "test"
        )
        nodeTemplateRepository.saveNode(nodeTmp)

        val saveResponse: Node = nodeTemplateRepository.findNodeByRegionName("test")

        assertThat(saveResponse.ipAddress).isEqualTo(nodeTmp.ipAddress)
        assertThat(saveResponse.hostName).isEqualTo(nodeTmp.hostName)
        assertThat(saveResponse.hostPort).isEqualTo(nodeTmp.hostPort)
    }
}