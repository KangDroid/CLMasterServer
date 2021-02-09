package com.kangdroid.master.data.node.dto

import com.kangdroid.master.data.node.Node

/**
 * NodeSaveRequestDto: Used when Admin registers compute node to master server.
 *
 * variables:
 * id for db-specific
 * hostName for nickname of the server
 * hostPort for compute-server's port
 * ipAddress for compute-server's IP Address.
 * [regionName] will be initialized after creating dto.
 */
class NodeSaveRequestDto(
        var id: Long,
        var hostName: String,
        var hostPort: String,
        var ipAddress: String
) {
    fun toEntity(): Node {
        return Node(
                id = this.id,
                hostName = this.hostName,
                ipAddress = this.ipAddress,
                hostPort = this.hostPort,
                regionName = "TMP_REGION"
        )
    }
}