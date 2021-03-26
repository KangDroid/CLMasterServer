package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.*
import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserTemplateRepository
import com.kangdroid.master.data.user.dto.*
import com.kangdroid.master.error.exception.ConflictException
import com.kangdroid.master.error.exception.ForbiddenException
import com.kangdroid.master.error.exception.NotFoundException
import com.kangdroid.master.error.exception.UnknownErrorException
import com.kangdroid.master.security.JWTTokenProvider
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService {
    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var nodeService: NodeService

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    @Autowired
    private lateinit var passwordEncoder: PasswordEncryptorService

    // Default Logger
    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    fun getUserInformation(token: String): ResponseEntity<UserInformationResponseDto> {
        val userName: String = getUserName(token) ?: run {
            logger.error("Token is acquired but cannot find user.")
            throw NotFoundException("Cannot find user!")
        }
        val userEntity: User = userTemplateRepository.findByUserName(userName)

        logger.debug("getUserInformation succeed. It should return OK sign with correct body now.")
        return ResponseEntity.ok(
            UserInformationResponseDto(
                userName = userEntity.userName,
                userRole = userEntity.roles
            )
        )
    }

    fun getUserName(token: String): String? {
        var userName: String? = null
        runCatching {
            userName = jwtTokenProvider.getUserPk(token)
        }.onFailure {
            logger.error("Error occurred when getting username!")
            logger.error("StackTrace: ${it.stackTraceToString()}")
            userName = null
        }

        return userName
    }

    fun registerUser(userRegisterDto: UserRegisterDto): ResponseEntity<UserRegisterResponseDto> {
        lateinit var userRegisterResponseDto: UserRegisterResponseDto

        runCatching {
            userTemplateRepository.findByUserName(userRegisterDto.userName)
        }.onSuccess {
            // Shouldn't success though
            logger.info("Username ${userRegisterDto.userName} does exists, CONFLICT")
            throw ConflictException("E-Mail address is already registered!")
        }.onFailure {
            // This is intended.
            logger.info("Username is not found. Continue to register.")
        }

        runCatching {
            userTemplateRepository.saveUser(
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
            logger.error("Error occurred when registering user!")
            logger.error("StackTrace: ${it.stackTraceToString()}")
            throw UnknownErrorException("Unknown Throw: ${it.cause.toString()}")
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(userRegisterResponseDto)
    }

    fun loginUser(userLoginRequestDto: UserLoginRequestDto): ResponseEntity<UserLoginResponseDto> {
        // UserLoginResponseDto
        val user: User = userTemplateRepository.findByUserName(userLoginRequestDto.userName)

        runCatching {
            require(passwordEncoder.isMatching(userLoginRequestDto.userPassword, user.password)) { "Wrong Password" }
        }.onFailure {
            logger.error("Error occurred when loginUser called!")
            logger.error("StackTrace: ${it.stackTraceToString()}")
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
        val user: User = runCatching {
            userTemplateRepository.findByUserName(userName)
        }.getOrElse {
            return "Cannot Find User. Please Re-Login"
        }

        // TODO: Just insert user's document by query
        user.dockerImage.add(
            DockerImage(
                dockerId = userImageResponseDto.containerId,
                computeRegion = userImageResponseDto.regionLocation,
            )
        )
        userTemplateRepository.saveUser(user)
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
        val user: User = userTemplateRepository.findByUserName(userName)

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

        val targetDockerImage: DockerImage = userTemplateRepository.findDockerImageByContainerID(userName, userRestartRequestDto.containerId)

        val errorMessage: String = nodeService.restartContainer(targetDockerImage)
        if (errorMessage.isNotEmpty()) {
            throw UnknownErrorException(errorMessage)
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(null)
    }
}