package com.kangdroid.master.data.node.dto

class NodeAliveResponseDto(
        var isDockerServerRunning: Boolean = false,
        var errorMessage: String = ""
)