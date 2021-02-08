package com.kangdroid.master.service

import com.kangdroid.master.data.node.Node
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class NodeService {
    @Autowired
    private lateinit var nodeRepository: NodeRepository

    fun save(nodeSaveRequestDto: NodeSaveRequestDto): String {
        val node: Node = nodeSaveRequestDto.toEntity()
        node.regionName = "Region-${nodeRepository.count()}"

        // Check for node integrity
        return if (isNodeRunning(nodeSaveRequestDto)) {
            nodeRepository.save(node).regionName
        } else {
            "Error"
        }
    }

    private fun isNodeRunning(nodeSaveRequestDto: NodeSaveRequestDto): Boolean {
        val restTemplate: RestTemplate = RestTemplate()
        val url: String = "http://${nodeSaveRequestDto.ipAddress}:${nodeSaveRequestDto.hostPort}/api/node/load"
        val responseEntity: ResponseEntity<String> = try {
            restTemplate.getForEntity(url, String::class.java)
        } catch (e: Exception) {
            println(e.stackTraceToString())
            return false
        }

        return (responseEntity.statusCode == HttpStatus.OK)
    }
}