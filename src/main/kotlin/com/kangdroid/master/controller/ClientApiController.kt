package com.kangdroid.master.controller

import com.kangdroid.master.data.docker.dto.*
import com.kangdroid.master.data.node.dto.NodeLoadResponseDto
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import com.kangdroid.master.service.NodeService
import com.kangdroid.master.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

@RestController
class ClientApiController {

    @Autowired
    private lateinit var nodeService: NodeService

    @Autowired
    private lateinit var userService: UserService

    /**
     * getNodeLoad(): Get All of Registered Node Load as List.
     * Returns: List of <NodeLoadResponseDto>[containing Load information]
     */
    @GetMapping("/api/client/node/load")
    fun getNodeLoad(): List<NodeLoadResponseDto> {
        return nodeService.getNodeLoad()
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
    fun getClientContainerList(@RequestHeader("userToken") userToken: String): List<UserImageListResponseDto> {
        if (!userService.checkToken(userToken)) {
            return listOf<UserImageListResponseDto>(UserImageListResponseDto(errorMessage = "Token is Invalid. Please Re-Login"))
        }
        return userService.listContainer(userToken)
    }

    /**
     * createContainer(param userImageSaveRequestDto): Create Container with appropriate Token
     * returns: UserImageResponseDto with IP/Port/ContainerID/Location
     * returns: UserImageResponseDto with errorMessage.
     */
    @PostMapping("/api/client/container")
    fun createContainer(@RequestBody userImageSaveRequestDto: UserImageSaveRequestDto): UserImageResponseDto {
        if (!userService.checkToken(userImageSaveRequestDto.userToken))
            return UserImageResponseDto(errorMessage = "Token is Invalid. Please Re-Login")

        return nodeService.createContainer(userImageSaveRequestDto)
    }

    /**
     * registerUser(param userRegisterDto): Create an user with given id/pw
     * Returns: Registered Id with userRegisterResponseDto
     * Returns: errorMessage With userRegisterResponseDto
     */
    @PostMapping("/api/client/register")
    fun registerUser(@RequestBody userRegisterDto: UserRegisterDto): UserRegisterResponseDto {
        return userService.registerUser(userRegisterDto)
    }

    @PostMapping("/api/client/login")
    fun loginUser(@RequestBody userImageLoginRequestDto: UserLoginRequestDto): UserLoginResponseDto {
        val servletRequest: HttpServletRequest =
            (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        var fromIp: String? = servletRequest.getHeader("X-FORWARDED-FOR")
        if (fromIp == null) {
            fromIp = servletRequest.remoteAddr
        }
        return userService.login(userImageLoginRequestDto, fromIp!!)
    }

    /**
     * restartContainerNode(): Restart User's Container
     */
    @PostMapping("/api/client/restart")
    fun restartContainerNode(@RequestBody userRestartRequestDto: UserRestartRequestDto): UserRestartResponseDto {
        if (!userService.checkToken(userRestartRequestDto.userToken))
            return UserRestartResponseDto(errorMessage = "Token is Invalid. Please Re-Login")

        return userService.restartContainer(userRestartRequestDto)
    }
}