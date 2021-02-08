package com.kangdroid.master.data.node

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class NodeEntityTest {

    @Autowired
    private lateinit var nodeEntity: NodeRepository

    @After
    fun clearAll() {
        nodeEntity.deleteAll()
    }

    @Test
    fun isSaveWorks() {
        // Let
        val nodeTmp: Node = Node(
                id = 1000,
                hostName = "localhost",
                ipAddress = "192.168.0.8",
                regionName = ""
        )

        // save
        nodeEntity.save(nodeTmp)

        // Assert
        assertThat(nodeEntity.count()).isEqualTo(1L)
    }
}