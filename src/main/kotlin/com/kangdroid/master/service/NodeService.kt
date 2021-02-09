package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImageRepository
import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
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

    @Autowired
    private lateinit var dockerImageRepository: DockerImageRepository

    fun createContainer(userImageSaveRequestDto: UserImageSaveRequestDto): String {
        val node: Node = nodeRepository.findByRegionName(userImageSaveRequestDto.computeRegion) ?: return "Error"

        val restTemplate: RestTemplate = RestTemplate()
        val url: String = "http://${node.ipAddress}:${node.hostPort}/api/node/image"
        val responseEntity: ResponseEntity<String> = try {
            restTemplate.postForEntity(url, String::class.java)
        } catch (e: Exception) {
            println(e.stackTraceToString())
            return "Error"
        }

        return if (responseEntity.body != null) {
            userImageSaveRequestDto.dockerId = responseEntity.body!!
            dockerImageRepository.save(userImageSaveRequestDto.toEntity())
            responseEntity.body!!
        } else {
            "Error"
        }
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