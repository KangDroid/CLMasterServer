package com.kangdroid.master.service

import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
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

    @After
    fun clearAllRepo() {
        nodeRepository.deleteAll()
    }

    @Test
    fun isSavingWorksWell() {
        // Let
        val nodeSaveRequestDto: NodeSaveRequestDto = NodeSaveRequestDto(
                id = 10,
                hostName = "testing",
                ipAddress = "testing"
        )

        // do work
        val returnValue: String = nodeService.save(nodeSaveRequestDto)

        // Assert
        assertThat(returnValue.length).isGreaterThan(0)
        assertThat(returnValue).isEqualTo("Region-${nodeRepository.count() - 1}")
    }
}