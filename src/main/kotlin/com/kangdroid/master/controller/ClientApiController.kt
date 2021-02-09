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

    /**
     * getNodeLoad(): Get All of Registered Node Load as List.
     * Returns: List of <NodeLoadResponseDto>[containing Load information]
     */
    @GetMapping("/api/client/node/load")
    fun getNodeLoad(): List<NodeLoadResponseDto> {
        return nodeService.getNodeLoad()
    }

    /**
     * registerUserDocker(param dto): Create User with Docker Image Creation
     * Basically calling this api will create an unique-docker image[with openssh enabled], with registering user.
     * returns: A String, containing Docker Container ID
     * returns: A String, containing "Error" for error.
     */
    @PostMapping("/api/client/register")
    fun registerUserDocker(@RequestBody userImageSaveRequestDto: UserImageSaveRequestDto): String {
        return nodeService.createContainer(userImageSaveRequestDto)
    }
}