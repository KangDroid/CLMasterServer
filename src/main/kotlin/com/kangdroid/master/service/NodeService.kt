package com.kangdroid.master.service

import com.kangdroid.master.data.node.Node
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeLoadResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.util.stream.Collectors

@Service
class NodeService {
    @Autowired
    private lateinit var nodeRepository: NodeRepository

    fun createContainer(region: String): String {
        val node: Node = nodeRepository.findByRegionName(region) ?: return "Error"

        val restTemplate: RestTemplate = RestTemplate()
        val url: String = "http://${node.ipAddress}:${node.hostPort}/api/node/image"
        val responseEntity: ResponseEntity<String> = try {
            restTemplate.postForEntity(url, String::class.java)
        } catch (e: Exception) {
            println(e.stackTraceToString())
            return "Error"
        }

        return responseEntity.body ?: "Error"
    }

    fun getNodeLoad(): List<NodeLoadResponseDto> {
        return nodeRepository.findAll().stream()
                .map {NodeLoadResponseDto(it)}
                .collect(Collectors.toList())
    }

    fun save(nodeSaveRequestDto: NodeSaveRequestDto): String {
        val node: Node = nodeSaveRequestDto.toEntity()
        node.regionName = "Region-${nodeRepository.count()}"

        val nodeGot: Node = nodeRepository.findByIpAddress(node.ipAddress) ?: Node(id = Long.MAX_VALUE, "", "", "", "")
        if (nodeGot.id != Long.MAX_VALUE) {
            return "Error"
        }

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