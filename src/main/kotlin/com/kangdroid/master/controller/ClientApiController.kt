package com.kangdroid.master.controller

import com.kangdroid.master.data.docker.dto.*
import com.kangdroid.master.data.node.dto.NodeInformationResponseDto
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import com.kangdroid.master.error.Response
import com.kangdroid.master.service.NodeService
import com.kangdroid.master.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class ClientApiController {

    @Autowired
    private lateinit var nodeService: NodeService

    @Autowired
    private lateinit var userService: UserService

    // Just for testing with postman
    @PostMapping("/api/client/register")
    fun register(@RequestBody userRegisterDto: UserRegisterDto): ResponseEntity<UserRegisterResponseDto> {
        return userService.registerUser(userRegisterDto)
    }

    @PostMapping("/api/client/login")
    fun login(@RequestBody userLoginRequestDto: UserLoginRequestDto): ResponseEntity<UserLoginResponseDto> {
        return userService.loginUser(userLoginRequestDto)
    }

    /**
     * getNodeLoad(): Get All of Registered Node Load as List.
     * Returns: List of <NodeLoadResponseDto>[containing Load information]
     */
    @GetMapping("/api/client/node")
    fun getNodeInformation(): ResponseEntity<List<NodeInformationResponseDto>> {
        return nodeService.getNodeInformation()
    }

    /**
     * isMasterServerAlive(): Check whether master server is alive[for client-request]
     */
    @GetMapping("/api/client/alive")
    fun isMasterServerAlive(): Boolean = true

    /**
     * getNodeListUser(param token) Get List of node for corresponding user.
     */
    @GetMapping("/api/client/container")
    fun getClientContainerList(@RequestHeader httpHeaders: HttpHeaders): ResponseEntity<List<UserImageListResponseDto>> {
        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
        return userService.listContainer(tokenList[0])
    }

    /**
     * createContainer(param userImageSaveRequestDto): Create Container with appropriate Token
     * returns: UserImageResponseDto with IP/Port/ContainerID/Location
     * returns: UserImageResponseDto with errorMessage.
     */
    @PostMapping("/api/client/container")
    fun createContainer(
        @RequestBody userImageSaveRequestDto: UserImageSaveRequestDto,
        @RequestHeader httpHeaders: HttpHeaders
    ): ResponseEntity<UserImageResponseDto> {
        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
        userImageSaveRequestDto.userToken = tokenList[0]

        return nodeService.createContainer(userImageSaveRequestDto)
    }

    /**
     * restartContainerNode(): Restart User's Container
     */
    @PostMapping("/api/client/restart")
    fun restartContainerNode(
        @RequestBody userRestartRequestDto: UserRestartRequestDto,
        @RequestHeader httpHeaders: HttpHeaders
    ): UserRestartResponseDto {
        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
        userRestartRequestDto.userToken = tokenList[0]

        return userService.restartContainer(userRestartRequestDto)
    }
}