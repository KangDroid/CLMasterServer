package com.kangdroid.master.data.node.dto

import com.kangdroid.master.data.node.Node
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class NodeLoadResponseDto(
        var regionName: String,
        var nodeLoadPercentage: String = ""
) {
    constructor(entity: Node): this(regionName = entity.regionName) {
        val restTemplate: RestTemplate = RestTemplate()
        val url: String = "http://${entity.ipAddress}:${entity.hostPort}/api/node/load"
        val responseEntity: ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)

        // TODO: Exception --> Node is down.
        this.nodeLoadPercentage = responseEntity.body ?: "Server is down"
    }
}