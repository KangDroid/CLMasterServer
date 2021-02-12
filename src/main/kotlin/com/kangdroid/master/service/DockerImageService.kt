package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.DockerImageRepository
import com.kangdroid.master.data.docker.dto.UserImageLoginRequestDto
import com.kangdroid.master.data.docker.dto.UserImageLoginResponseDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

@Service
class DockerImageService {
    @Autowired
    private lateinit var dockerImageRepository:DockerImageRepository

    @Autowired
    private lateinit var passwordEncryptorService: PasswordEncryptorService

    /**
     * saveWithCheck(param entity): Save User DB with checking duplication
     * Returns: Empty String
     * Returns: An Error Message
     */
    fun saveWithCheck(entity: DockerImage): String {
        if (isDuplicateIDExists(entity.userName)) {
            return "Requested ID[${entity.userName}] already exists!"
        }

        dockerImageRepository.save(entity)

        return ""
    }

    /**
     * login(param userImageLoginRequestDto): Log-In, Create Custom Session Token
     * Returns: Full UserImageLoginResponse with Session Token
     * Returns: UserImageLoginResponse with Error Message in it.
     */
    fun login(userImageLoginRequestDto: UserImageLoginRequestDto, ip: String): UserImageLoginResponseDto {
        val errorMessage: String = "Either ID/PW is Incorrect!"
        // 1. Get User Id + Docker Image Entity
        val dockerImage: DockerImage = dockerImageRepository.findByUserName(userImageLoginRequestDto.userName)
                ?: return UserImageLoginResponseDto(errorMessage = errorMessage)
        // 2. Check whether requested PW equals DB's Password
        return if (passwordEncryptorService.isMatching(userImageLoginRequestDto.userPassword, dockerImage.userPassword)) {
            // Create Token
            dockerImage.userToken = createToken(dockerImage, ip)
            dockerImageRepository.save(dockerImage) // edit db
            UserImageLoginResponseDto(token = dockerImage.userToken)
        } else {
            UserImageLoginResponseDto(errorMessage = errorMessage)
        }
    }

    /**
     * Create Token based on: User Name, Password, Current Server Time, Expiration Time, Ip
     */
    fun createToken(dockerImage: DockerImage, ip: String): String {
        val finalString: String = getSHA512(dockerImage.userName) + getSHA512(dockerImage.userPassword) +
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
        return (dockerImageRepository.findByUserName(userId) != null)
    }
}