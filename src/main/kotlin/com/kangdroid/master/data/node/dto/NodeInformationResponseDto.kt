package com.kangdroid.master.data.node.dto

import com.kangdroid.master.data.node.Node
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

/**
 * NodeLoadResponseDto
 *
 * Information about each node's Load data.
 *
 * Variables:
 * regionName for Compute-region[device identifier]
 * nodeLoadPercentage for Node Load Percentage in last 1-minute.
 */
class NodeInformationResponseDto(
    var regionName: String = "",
    var nodeLoadPercentage: String = ""
) {
    constructor(entity: Node, restTemplate: RestTemplate) : this(regionName = entity.regionName) {
        // When we get fresh node data from Entity, thus we have to convert entity to this dto.
        // So, when we converting, we check load information!
        val url: String = "http://${entity.ipAddress}:${entity.hostPort}/api/node/load"
        val responseEntity: ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)

        // TODO: Exception --> Node is down.
        this.nodeLoadPercentage = responseEntity.body ?: "Server is down"
    }
}