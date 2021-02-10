package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.DockerImageRepository
import com.kangdroid.master.data.docker.dto.UserImageResponseDto
import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.node.Node
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeLoadResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.util.stream.Collectors
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Service
class NodeService {
    @Autowired
    private lateinit var nodeRepository: NodeRepository

    @Autowired
    private lateinit var dockerImageRepository: DockerImageRepository

    /**
     * createContainer(param dto): Create Container in Compute Node's Server, with given DTO
     * Param: UserImageSaveRequestDto[id, password, docker-ID, compute-region]
     * returns: UserImageResponseDto - Containing Full information about container
     * returns: UserImageResponseDto - Containing Error Message.
     */
    fun createContainer(userImageSaveRequestDto: UserImageSaveRequestDto): UserImageResponseDto {
        // Find Compute Node information given DTO - to register image on that container.
        val node: Node = nodeRepository.findByRegionName(userImageSaveRequestDto.computeRegion) ?: run {
            return UserImageResponseDto(errorMessage = "Cannot find Compute Region!")
        }

        // Request compute-node to create a fresh container
        val restTemplate: RestTemplate = RestTemplate()
        val url: String = "http://${node.ipAddress}:${node.hostPort}/api/node/image"
        val responseEntity: ResponseEntity<UserImageResponseDto> = try {
            restTemplate.postForEntity(url, UserImageResponseDto::class.java)
        } catch (e: Exception) {
            println(e.stackTraceToString())
            return UserImageResponseDto(errorMessage = "Cannot communicate with Compute node!")
        }
        val userImageResponseDto: UserImageResponseDto = responseEntity.body?.also {
            it.regionLocation = userImageSaveRequestDto.computeRegion
        } ?: UserImageResponseDto(errorMessage = "Getting Response from Compute Node failed!")

        dockerImageRepository.save(DockerImage(
                userName = userImageSaveRequestDto.userName,
                userPassword = userImageSaveRequestDto.userPassword,
                dockerId = userImageResponseDto.containerId,
                computeRegion = userImageResponseDto.regionLocation
        ))

        return userImageResponseDto
    }

    /**
     * getNodeLoad(): Get All of load information, in registered node on master's db.
     * returns: List of <NodeLoadResponseDto>[Containing Region, Load Info]
     */
    fun getNodeLoad(): List<NodeLoadResponseDto> {
        return nodeRepository.findAll().stream()
                .map {NodeLoadResponseDto(it)}
                .collect(Collectors.toList())
    }

    /**
     * save(param dto): Save Node Information[Register] to db.
     * this function will check whether node api server is actually running and save it to actual db.
     *
     * returns: A NodeSaveResponseDto, containing Node Information
     * returns: A NodeSaveResponseDto, containing errorMessage.
     */
    fun save(nodeSaveRequestDto: NodeSaveRequestDto): NodeSaveResponseDto {
        // Convert DTO to Entity
        val node: Node = nodeSaveRequestDto.toEntity()

        // Set Region Name based on db's count
        node.regionName = "Region-${nodeRepository.count()}"

        // Find Any duplicated registered node. - if duplicated node found, return "Error"
        val nodeGot: Node = nodeRepository.findByIpAddress(node.ipAddress) ?: Node(id = Long.MAX_VALUE, "", "", "", "")
        if (nodeGot.id != Long.MAX_VALUE) {
            return NodeSaveResponseDto(errorMessage = "Duplicated Compute Node is found on IP Address: ${node.ipAddress}")
        }

        // Check for node integrity
        return if (isNodeRunning(nodeSaveRequestDto)) {
            NodeSaveResponseDto(nodeRepository.save(node))
        } else {
            NodeSaveResponseDto(errorMessage = "Node Server is NOT Running. Check IP Address/Server Port")
        }
    }

    /**
     * isNodeRunning(param dto): Check whether Specified compute node is actually working
     * This function will call REST::GET API on compute-node server.
     *
     * returns: true - when node is actually working.
     * returns: false - when any exception occurs[or internal server error].
     */
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