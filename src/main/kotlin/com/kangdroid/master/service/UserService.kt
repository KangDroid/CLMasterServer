package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.*
import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.*
import com.kangdroid.master.error.exception.ConflictException
import com.kangdroid.master.error.exception.ForbiddenException
import com.kangdroid.master.error.exception.NotFoundException
import com.kangdroid.master.error.exception.UnknownErrorException
import com.kangdroid.master.security.JWTTokenProvider
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    @Autowired
    private lateinit var passwordEncoder: PasswordEncryptorService

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

    fun registerUser(userRegisterDto: UserRegisterDto): ResponseEntity<UserRegisterResponseDto> {
        lateinit var userRegisterResponseDto: UserRegisterResponseDto
        runCatching {
            userRepository.save(
                User(
                    userName = userRegisterDto.userName,
                    userPassword = passwordEncoder.encodePlainText(userRegisterDto.userPassword),
                    roles = setOf("ROLE_USER")
                )
            )
        }.onSuccess {
            userRegisterResponseDto = UserRegisterResponseDto(
                registeredId = it.userName,
            )
        }.onFailure {
            if (it.cause is ConstraintViolationException) {
                throw ConflictException("E-Mail address is already registered!")
            } else {
                throw UnknownErrorException("Unknown Throw: ${it.cause.toString()}")
            }
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(userRegisterResponseDto)
    }

    fun loginUser(userLoginRequestDto: UserLoginRequestDto): ResponseEntity<UserLoginResponseDto> {
        // UserLoginResponseDto
        val user: User = userRepository.findByUserName(userLoginRequestDto.userName)
            ?: throw NotFoundException("Cannot find user: ${userLoginRequestDto.userName}")

        runCatching {
            require(passwordEncoder.isMatching(userLoginRequestDto.userPassword, user.password)) { "Wrong Password" }
        }.onFailure {
            throw ForbiddenException("Password is incorrect!")
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                UserLoginResponseDto(
                    token = jwtTokenProvider.createToken(userLoginRequestDto.userName, user.roles.toList())
                )
            )
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
    fun listContainer(userToken: String): ResponseEntity<List<UserImageListResponseDto>> {
        val userName: String = getUserName(userToken)
            ?: throw NotFoundException("Cannot Find User. Please Re-Login")
        val user: User = userRepository.findByUserName(userName)
            ?: throw NotFoundException("Cannot Find User. Please Re-Login")

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

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(mutableImageList.toList())
    }

    /**
     * RestartContainer(): Restart Corresponding Container
     */
    fun restartContainer(userRestartRequestDto: UserRestartRequestDto): ResponseEntity<Void> {
        val userName: String = getUserName(userRestartRequestDto.userToken)
            ?: throw NotFoundException("Cannot find user with token!")
        val user: User = userRepository.findByUserName(userName)
            ?: throw NotFoundException("Cannot find user with token!")
        val dockerImageList: MutableList<DockerImage> = user.dockerImage
        val targetDockerImage: DockerImage = dockerImageList.find {
            it.dockerId == userRestartRequestDto.containerId
        } ?: throw NotFoundException("Cannot find container ID!")

        val errorMessage: String = nodeService.restartContainer(targetDockerImage)
        if (errorMessage.isNotEmpty()) {
            throw UnknownErrorException(errorMessage)
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(null)
    }
}