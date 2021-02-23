package com.kangdroid.master.data.node.dto

import com.kangdroid.master.data.node.Node

class NodeSaveResponseDto(
    var ipAddress: String = "",
    var hostPort: String = "",
    var regionName: String = "",
    var errorMessage: String = ""
) {
    constructor(entity: Node) : this(
        ipAddress = entity.ipAddress,
        hostPort = entity.hostPort,
        regionName = entity.regionName
    )
}