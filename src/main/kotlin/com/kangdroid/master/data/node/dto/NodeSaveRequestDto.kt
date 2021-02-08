package com.kangdroid.master.data.node.dto

import com.kangdroid.master.data.node.Node

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