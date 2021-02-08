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
     * Register Compute Node
     * returns Node Region
     */
    @PostMapping("/api/admin/node/register")
    fun registerNode(@RequestBody nodeSaveRequestDto: NodeSaveRequestDto): String {
        return nodeService.save(nodeSaveRequestDto)
    }
}