package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.UserImageResponseDto
import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.node.Node
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeAliveResponseDto
import com.kangdroid.master.data.node.dto.NodeInformationResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.error.exception.ConflictException
import com.kangdroid.master.error.exception.NotFoundException
import com.kangdroid.master.error.exception.UnknownErrorException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.util.stream.Collectors
import javax.annotation.PostConstruct

@Service
class NodeService {
    @Autowired
    private lateinit var nodeRepository: NodeRepository

    @Autowired
    private lateinit var userService: UserService

    // Global Rest Template
    lateinit var restTemplate: RestTemplate

    @PostConstruct
    fun initRestTemplate() {
        val clientRequestFactory: HttpComponentsClientHttpRequestFactory =
            HttpComponentsClientHttpRequestFactory().also {
                it.setConnectTimeout(10 * 1000)
                it.setReadTimeout(10 * 1000)
            }
        restTemplate = RestTemplate(clientRequestFactory)
    }

    inner class Cause(
        var value: Boolean,
        var cause: String
    )

    /**
     * createContainer(param dto): Create Container in Compute Node's Server, with given DTO
     * Param: UserImageSaveRequestDto[id, password, docker-ID, compute-region]
     * returns: UserImageResponseDto - Containing Full information about container
     * returns: UserImageResponseDto - Containing Error Message.
     */
    fun createContainer(userImageSaveRequestDto: UserImageSaveRequestDto): ResponseEntity<UserImageResponseDto> {
        // Find Compute Node information given DTO - to register image on that container.
        val node: Node = nodeRepository.findByRegionName(userImageSaveRequestDto.computeRegion) ?: run {
            throw NotFoundException("Cannot find Compute Region!")
        }

        // Request compute-node to create a fresh container
        val url: String = "http://${node.ipAddress}:${node.hostPort}/api/node/image"
        val responseEntity: ResponseEntity<UserImageResponseDto> =
            runCatching<ResponseEntity<UserImageResponseDto>> {
                restTemplate.postForEntity(url, UserImageResponseDto::class.java)
            }.onFailure {
                println(it.stackTraceToString())
            }.getOrNull() ?: run {
                throw UnknownErrorException("Cannot communicate with Compute node!")
            }

        val userImageResponseDto: UserImageResponseDto = responseEntity.body!!
        userImageResponseDto.regionLocation = userImageSaveRequestDto.computeRegion

        // Save back to dockerImage DBData
        val checkResponse: String = userService.saveWithCheck(userImageSaveRequestDto.userToken, userImageResponseDto)

        if (checkResponse != "") {
            throw UnknownErrorException(checkResponse)
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(userImageResponseDto)
    }

    /**
     * restartContainer: Restart Container given Docker Image
     */
    fun restartContainer(dockerImage: DockerImage): String {
        val node: Node = nodeRepository.findByRegionName(dockerImage.computeRegion)
            ?: return "Cannot find Compute Region!"

        // Request compute-node to create a fresh container
        val url: String = "http://${node.ipAddress}:${node.hostPort}/api/node/restart"

        // Set Parameter[Use Multivalue since we are not global-fying dto object]
        class restartRequestDto(
            var containerId: String
        )

        val requestDto: restartRequestDto = restartRequestDto(dockerImage.dockerId)

        val responseEntity: ResponseEntity<String> = runCatching {
            restTemplate.postForEntity(url, requestDto, String::class.java)
        }.onFailure {
            println(it.stackTraceToString())
        }.getOrNull() ?: return "Cannot communicate with Compute node!"

        if (responseEntity.hasBody()) {
            return responseEntity.body!!
        }

        return ""
    }

    /**
     * getNodeLoad(): Get All of load information, in registered node on master's db.
     * returns: List of <NodeLoadResponseDto>[Containing Region, Load Info]
     */
    fun getNodeInformation(): ResponseEntity<List<NodeInformationResponseDto>> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(nodeRepository.findAll().stream()
                .map { NodeInformationResponseDto(it, restTemplate) }
                .collect(Collectors.toList())
            )
    }

    /**
     * save(param dto): Save Node Information[Register] to db.
     * this function will check whether node api server is actually running and save it to actual db.
     *
     * returns: A NodeSaveResponseDto, containing Node Information
     * returns: A NodeSaveResponseDto, containing errorMessage.
     */
    fun save(nodeSaveRequestDto: NodeSaveRequestDto): ResponseEntity<NodeSaveResponseDto> {
        // Convert DTO to Entity
        val node: Node = nodeSaveRequestDto.toEntity()

        // Set Region Name based on db's count
        node.regionName = "Region-${nodeRepository.count()}"

        // Find Any duplicated registered node. - if duplicated node found, return "Error"
        val nodeGot: Node = nodeRepository.findByIpAddress(node.ipAddress) ?: Node(id = Long.MAX_VALUE, "", "", "", "")
        if (nodeGot.id != Long.MAX_VALUE) {
            throw ConflictException("Duplicated Compute Node is found on IP Address: ${node.ipAddress}")
        }

        // Check for node integrity
        val result: Cause = isNodeRunning(nodeSaveRequestDto)

        return if (result.cause.isNotEmpty() || result.value) {
            throw UnknownErrorException(result.cause)
        } else {
            ResponseEntity
                .status(HttpStatus.OK)
                .body(NodeSaveResponseDto(nodeRepository.save(node)))
        }
    }

    /**
     * isNodeRunning(param dto): Check whether Specified compute node is actually working
     * This function will call REST::GET API on compute-node server.
     *
     * returns: true - when node is actually working.
     * returns: false - when any exception occurs[or internal server error].
     */
    private fun isNodeRunning(nodeSaveRequestDto: NodeSaveRequestDto): Cause {
        val url: String = "http://${nodeSaveRequestDto.ipAddress}:${nodeSaveRequestDto.hostPort}/api/alive"
        val responseEntity: ResponseEntity<NodeAliveResponseDto> = runCatching {
            restTemplate.getForEntity(url, NodeAliveResponseDto::class.java)
        }.onFailure {
            println(it.stackTraceToString())
        }.getOrNull() ?: return Cause(
            value = false,
            cause = "Connecting to Node Server failed. Check for IP/Port again."
        )

        val response: NodeAliveResponseDto = responseEntity.body!!

        return Cause(
            value = response.isDockerServerRunning,
            cause = response.errorMessage
        )
    }
}