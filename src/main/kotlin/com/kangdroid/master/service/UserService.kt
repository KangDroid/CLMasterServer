package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.dto.UserImageResponseDto
import com.kangdroid.master.data.docker.dto.UserImageSaveRequestDto
import com.kangdroid.master.data.user.User
import com.kangdroid.master.data.user.UserRepository
import com.kangdroid.master.data.user.dto.UserLoginRequestDto
import com.kangdroid.master.data.user.dto.UserLoginResponseDto
import com.kangdroid.master.data.user.dto.UserRegisterDto
import com.kangdroid.master.data.user.dto.UserRegisterResponseDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.*
import javax.annotation.PreDestroy
import javax.xml.bind.DatatypeConverter
import kotlin.concurrent.schedule

@Service
class UserService {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncryptorService: PasswordEncryptorService

    // Timer Class
    private val timer: Timer = Timer()

    // Token Expiration Time in Milliseconds
    private val tokenExpireTime: Long = 1000 * 60 * 3 // 60s

    @PreDestroy
    fun clearTime() {
        timer.cancel()
        timer.purge()
    }

    /**
     * saveWithCheck(param entity): Save DockerImage[Value] to corresponding user
     * Returns: Empty String
     * Returns: An Error Message
     */
    fun saveWithCheck(token: String, userImageResponseDto: UserImageResponseDto): String {
        val user: User = userRepository.findByUserToken(token)
            ?: return "Cannot Find User. Please Re-Login"

        user.dockerImage.add(DockerImage(
            dockerId = userImageResponseDto.containerId,
            computeRegion = userImageResponseDto.regionLocation,
            user = user
        ))
        userRepository.save(user)
        return ""
    }

    fun testing() {
        val userList: List<User> = userRepository.findAll()

        for (tmpUser in userList) {
            println("User: ${tmpUser.userName}")
            for (tmpList in tmpUser.dockerImage) {
                println(tmpList.dockerId)
            }
        }
    }

    /**
     * checkToken(): Check token is valid
     * Returns: True when token is valid
     * Returns: False when token is invalid
     */
    fun checkToken(userImageSaveRequestDto: UserImageSaveRequestDto): Boolean {
        return (userRepository.findByUserToken(userImageSaveRequestDto.userToken) != null)
    }

    /**
     * registerUser(param userRegisterDto): Register User with data userRegisterDto
     * returns: UserRegisterResponseDto with Registered ID
     * returns: UserRegisterResponseDto with Error Message
     */
    fun registerUser(userRegisterDto: UserRegisterDto): UserRegisterResponseDto {
        // Assert if duplicate username exists
        if (userRepository.findByUserName(userRegisterDto.userName) != null) {
            return UserRegisterResponseDto(errorMessage = "ID: ${userRegisterDto.userName} exists!")
        }

        // Save!
        userRepository.save(User(
            id = 0,
            userName = userRegisterDto.userName,
            userPassword = passwordEncryptorService.encodePlainText(userRegisterDto.userPassword),
        ))

        return UserRegisterResponseDto(userRegisterDto.userName, "")
    }

    /**
     * login(param userImageLoginRequestDto): Log-In, Create Custom Session Token
     * Returns: Full UserImageLoginResponse with Session Token
     * Returns: UserImageLoginResponse with Error Message in it.
     */
    fun login(userLoginRequestDto: UserLoginRequestDto, ip: String): UserLoginResponseDto {
        val errorMessage: String = "Either ID/PW is Incorrect!"
        // 1. Get User Information
        val user: User = userRepository.findByUserName(userLoginRequestDto.userName)
            ?: return UserLoginResponseDto(errorMessage = errorMessage)

        // 2. Check whether requested PW equals DB's Password
        return if (passwordEncryptorService.isMatching(userLoginRequestDto.userPassword, user.userPassword)) {
            // Create Token
            user.userToken = createToken(user, ip)
            user.userTokenExp = System.currentTimeMillis() + tokenExpireTime

            // Expiration Task
            timer.schedule(Date(user.userTokenExp)) {
                println("Token Expired!")
                user.userToken = ""
                user.userTokenExp = 0
                userRepository.save(user)
            }

            // Anyway - Edit Db[Save Token]
            userRepository.save(user) // edit db
            UserLoginResponseDto(token = user.userToken)
        } else {
            UserLoginResponseDto(errorMessage = errorMessage)
        }
    }

    /**
     * Create Token based on: User Name, Password, Current Server Time, Expiration Time, Ip
     */
    fun createToken(user: User, ip: String): String {
        val finalString: String = getSHA512(user.userName) + getSHA512(user.userPassword) +
                getSHA512(ip) + getSHA512(System.currentTimeMillis().toString())
        val finalArray: CharArray = finalString.toCharArray().also {
            it.shuffle()
        }

        return getSHA512(finalArray.joinToString(""))
    }

    /**
     * getSHA512(param input): Create SHA-256 String
     * returns: SHA-256 String
     */
    fun getSHA512(input: String): String {
        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-512").also {
            it.update(input.toByteArray())
        }
        return DatatypeConverter.printHexBinary(messageDigest.digest())
    }

    /**
     * isDuplicateIDExists(param id): Check whether duplicate ID exists with param id.
     * Returns: True when duplicated ID exists
     * Returns: False when it is new ID
     */
    private fun isDuplicateIDExists(userId: String): Boolean {
        return (userRepository.findByUserName(userId) != null)
    }
}