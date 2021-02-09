package com.kangdroid.master.controller

import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
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
     * returns: A String, containing registered region.
     * returns: A String, containing "Error"
     */
    @PostMapping("/api/admin/node/register")
    fun registerNode(@RequestBody nodeSaveRequestDto: NodeSaveRequestDto): String {
        return nodeService.save(nodeSaveRequestDto)
    }
}