package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.DockerImageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DockerImageService {
    @Autowired
    private lateinit var dockerImageRepository:DockerImageRepository

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
     * isDuplicateIDExists(param id): Check whether duplicate ID exists with param id.
     * Returns: True when duplicated ID exists
     * Returns: False when it is new ID
     */
    private fun isDuplicateIDExists(userId: String): Boolean {
        return (dockerImageRepository.findByUserName(userId) != null)
    }
}