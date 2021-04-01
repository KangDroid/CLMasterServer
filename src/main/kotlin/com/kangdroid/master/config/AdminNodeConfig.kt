package com.kangdroid.master.config

import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.service.NodeService
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import javax.annotation.PostConstruct

@ConstructorBinding
@ConfigurationProperties("admin-node")
data class AdminNodeConfig(val nodes: List<NodeConfiguration>?) {
    @Autowired
    private lateinit var nodeService: NodeService

    private val logger: Logger = LoggerFactory.getLogger(AdminNodeConfig::class.java)
    data class NodeConfiguration(
        val nodeIp: String?,
        val nodePort: String?
    ) {
        override fun toString(): String {
            return """
                Node IP: $nodeIp
                Node Port: $nodePort
            """.trimIndent()
        }
    }

    @PostConstruct
    fun initNode() {
        nodes?.let {
            it.forEach { eachNode ->
                logger.info("Saving Node info")
                logger.info("$eachNode")
                val response: ResponseEntity<NodeSaveResponseDto> =  nodeService.save(
                    NodeSaveRequestDto(
                        id = ObjectId(),
                        hostName = "",
                        ipAddress = eachNode.nodeIp!!,
                        hostPort = eachNode.nodePort!!
                    )
                )

                if (response.statusCode != HttpStatus.OK) {
                    logger.error("Saving node failed!!")
                    logger.error("Node Info")
                    logger.error("$eachNode")
                } else {
                    logger.info("Saving node succeed!")
                    logger.info("Region Name: ${response.body?.regionName}")
                }
            }
        }
    }
}