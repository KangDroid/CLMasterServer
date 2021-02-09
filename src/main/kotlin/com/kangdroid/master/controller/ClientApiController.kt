package com.kangdroid.master.controller

import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeLoadResponseDto
import com.kangdroid.master.service.NodeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ClientApiController {

    @Autowired
    private lateinit var nodeService: NodeService

    // Get Node load data
    @GetMapping("/api/client/node/load")
    fun getNodeLoad(): List<NodeLoadResponseDto> {
        return nodeService.getNodeLoad()
    }

    // Finally Register
    @PostMapping("/api/client/register")
    fun registerUserDocker(@RequestBody userImageSaveRequestDto: UserImageSaveRequestDto): String {
        return nodeService.createContainer(userImageSaveRequestDto.computeRegion)
    }
}