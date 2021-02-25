package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.*
import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.*
import com.kangdroid.master.security.JWTTokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var nodeService: NodeService

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    fun getUserName(token: String): String? {
        var userName: String? = null
        runCatching {
            userName = jwtTokenProvider.getUserPk(token)
        }.onFailure {
            println(it.stackTraceToString())
            userName = null
        }

        return userName
    }

    /**
     * saveWithCheck(param entity): Save DockerImage[Value] to corresponding user
     * Returns: Empty String
     * Returns: An Error Message
     */
    fun saveWithCheck(token: String, userImageResponseDto: UserImageResponseDto): String {
        val userName: String = getUserName(token)
            ?: return "Cannot Find User. Please Re-Login"
        val user: User = userRepository.findByUserName(userName)
            ?: return "Cannot Find User. Please Re-Login"

        user.dockerImage.add(
            DockerImage(
                dockerId = userImageResponseDto.containerId,
                computeRegion = userImageResponseDto.regionLocation,
                user = user
            )
        )
        userRepository.save(user)
        return ""
    }

    /**
     * ListNode(param token): List User's Node information.
     * Returns Zero or more lists of nodes
     * Returns One DTO with errorMessage.
     */
    fun listContainer(userToken: String): List<UserImageListResponseDto> {
        val userName: String = getUserName(userToken)
            ?: return listOf(
                UserImageListResponseDto("", "", "", "Cannot Find User. Please Re-Login")
            )
        val user: User = userRepository.findByUserName(userName)
            ?: return listOf(
                UserImageListResponseDto("", "", "", "Cannot Find User. Please Re-Login")
            )

        val mutableImageList: MutableList<UserImageListResponseDto> = mutableListOf()
        for (dockerImage in user.dockerImage) {
            mutableImageList.add(
                UserImageListResponseDto(
                    userName = user.userName,
                    dockerId = dockerImage.dockerId,
                    computeRegion = dockerImage.computeRegion
                )
            )
        }

        return mutableImageList.toList()
    }

    /**
     * RestartContainer(): Restart Corresponding Container
     */
    fun restartContainer(userRestartRequestDto: UserRestartRequestDto): UserRestartResponseDto {
        val userName: String = getUserName(userRestartRequestDto.userToken)
            ?: return UserRestartResponseDto(errorMessage = "Cannot find user with token!")
        val user: User = userRepository.findByUserName(userName)
            ?: return UserRestartResponseDto(errorMessage = "Cannot find user with token!")
        val dockerImageList: MutableList<DockerImage> = user.dockerImage
        val targetDockerImage: DockerImage = dockerImageList.find {
            it.dockerId == userRestartRequestDto.containerId
        } ?: return UserRestartResponseDto(errorMessage = "Cannot find container ID!")

        val errorMessage: String = nodeService.restartContainer(targetDockerImage)
        if (errorMessage.isNotEmpty()) {
            return UserRestartResponseDto(errorMessage = errorMessage)
        }

        return UserRestartResponseDto()
    }
}