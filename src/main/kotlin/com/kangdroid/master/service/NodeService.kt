package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.UserImageResponseDto
import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.node.Node
import com.kangdroid.master.data.node.NodeTemplateRepository
import com.kangdroid.master.data.node.dto.NodeAliveResponseDto
import com.kangdroid.master.data.node.dto.NodeInformationResponseDto
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.error.exception.ConflictException
import com.kangdroid.master.error.exception.UnknownErrorException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    private lateinit var nodeTemplateRepository: NodeTemplateRepository

    @Autowired
    private lateinit var userService: UserService

    // Global Rest Template
    lateinit var restTemplate: RestTemplate

    // Default Logger
    private val logger: Logger = LoggerFactory.getLogger(NodeService::class.java)

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
        logger.info("createContainer started for user: ${userImageSaveRequestDto.userToken}")
        // Find Compute Node information given DTO - to register image on that container.
        val node: Node = nodeTemplateRepository.findNodeByRegionName(userImageSaveRequestDto.computeRegion)

        // Request compute-node to create a fresh container
        logger.info("Requesting to node server...")
        val url: String = "http://${node.ipAddress}:${node.hostPort}/api/node/image"
        val responseEntity: ResponseEntity<UserImageResponseDto> =
            runCatching<ResponseEntity<UserImageResponseDto>> {
                restTemplate.postForEntity(url, UserImageResponseDto::class.java)
            }.onFailure {
                logger.error("Requesting to node server failed.")
                logger.error("The Stack Trace:")
                logger.error(it.stackTraceToString())
            }.getOrNull() ?: run {
                throw UnknownErrorException("Cannot communicate with Compute node!")
            }

        val userImageResponseDto: UserImageResponseDto = responseEntity.body!!
        userImageResponseDto.regionLocation = userImageSaveRequestDto.computeRegion

        // Save back to dockerImage DBData
        logger.info("Saving image information to DB Data")
        val checkResponse: String = userService.saveWithCheck(userImageSaveRequestDto.userToken, userImageResponseDto)

        if (checkResponse != "") {
            logger.error("Error occurred when saving information to DB!")
            logger.error(checkResponse)
            throw UnknownErrorException(checkResponse)
        }

        logger.info("Successfully created container: ${userImageResponseDto.containerId}")
        logger.info("Returning entity for user: ${userImageSaveRequestDto.userToken}")
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(userImageResponseDto)
    }

    /**
     * restartContainer: Restart Container given Docker Image
     */
    fun restartContainer(dockerImage: DockerImage): String {
        logger.info("Restarting container requested for ${dockerImage.dockerId}")

        val node: Node = runCatching {
            nodeTemplateRepository.findNodeByRegionName(dockerImage.computeRegion)
        }.getOrElse {
            return "Cannot find Compute Region!"
        }

        // Request compute-node to create a fresh container
        val url: String = "http://${node.ipAddress}:${node.hostPort}/api/node/restart"

        // Set Parameter[Use Multivalue since we are not global-fying dto object]
        class restartRequestDto(
            var containerId: String
        )

        logger.info("Sending restarting request to node server.")
        val requestDto: restartRequestDto = restartRequestDto(dockerImage.dockerId)

        val responseEntity: ResponseEntity<String> = runCatching {
            restTemplate.postForEntity(url, requestDto, String::class.java)
        }.onFailure {
            logger.error("Error occurred when requesting restart on node server.")
            logger.error(it.stackTraceToString())
        }.getOrNull() ?: return "Cannot communicate with Compute node!"

        if (responseEntity.hasBody()) {
            logger.error("Seems like restarting request somehow failed!")
            logger.error(responseEntity.body)
            return responseEntity.body!!
        }

        logger.info("Restarting container for ${dockerImage.dockerId} succeed!")
        return ""
    }

    /**
     * getNodeLoad(): Get All of load information, in registered node on master's db.
     * returns: List of <NodeLoadResponseDto>[Containing Region, Load Info]
     */
    fun getNodeInformation(): ResponseEntity<List<NodeInformationResponseDto>> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(nodeTemplateRepository.findAll().stream()
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
        logger.info("Node save requested for ${nodeSaveRequestDto.ipAddress}")

        // Convert DTO to Entity
        val node: Node = nodeSaveRequestDto.toEntity()

        // Set Region Name based on db's count
        node.regionName = "Region-${nodeTemplateRepository.count()}"

        // Find Any duplicated registered node. - if duplicated node found, return "Error"
        logger.info("Finding any duplicated region..")
        runCatching {
            nodeTemplateRepository.findNodeByIpAddress(node.ipAddress)
        }.onSuccess {
            logger.error("Duplicated compute node is found on IP Address: ${node.ipAddress}")
            throw ConflictException("Duplicated Compute Node is found on IP Address: ${node.ipAddress}")
        }.onFailure {
            // intended
        }

        // Check for node integrity
        logger.info("Checking node integrity")
        val result: Cause = isNodeRunning(nodeSaveRequestDto)

        return if (result.cause.isNotEmpty() || result.value) {
            logger.error("Node failed or network connection exception!")
            logger.error(result.cause)
            throw UnknownErrorException(result.cause)
        } else {
            logger.info("Succeed to save node for ${nodeSaveRequestDto.ipAddress}!")
            ResponseEntity
                .status(HttpStatus.OK)
                .body(NodeSaveResponseDto(nodeTemplateRepository.saveNode(node)))
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
        logger.info("Checking whether node is actually running..[node: ${nodeSaveRequestDto.ipAddress}]")
        val url: String = "http://${nodeSaveRequestDto.ipAddress}:${nodeSaveRequestDto.hostPort}/api/alive"
        val responseEntity: ResponseEntity<NodeAliveResponseDto> = runCatching {
            restTemplate.getForEntity(url, NodeAliveResponseDto::class.java)
        }.onFailure {
            logger.error("Error occurred when checking node is running.")
            logger.error(it.stackTraceToString())
        }.getOrNull() ?: return Cause(
            value = false,
            cause = "Connecting to Node Server failed. Check for IP/Port again."
        )

        val response: NodeAliveResponseDto = responseEntity.body!!

        logger.info("Checking node running completed for ${nodeSaveRequestDto.ipAddress}!")
        return Cause(
            value = response.isDockerServerRunning,
            cause = response.errorMessage
        )
    }
}