package com.kangdroid.master.controller

import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import com.kangdroid.master.data.node.dto.NodeSaveResponseDto
import com.kangdroid.master.service.NodeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminApiController {

    @Autowired
    private lateinit var nodeService: NodeService

    /**
     * registerNode(param dto): Register compute node specified on DTO to server.
     * Calling this api will register node information[with verification] to master's node db.
     * returns: A NodeSaveResponseDto, containing Node Information
     * returns: A NodeSaveResponseDto, containing errorMessage.
     */
    @PostMapping("/api/admin/node/register")
    fun registerNode(@RequestBody nodeSaveRequestDto: NodeSaveRequestDto): NodeSaveResponseDto {
        return nodeService.save(nodeSaveRequestDto)
    }
}