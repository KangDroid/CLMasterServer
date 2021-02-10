package com.kangdroid.master.data.docker

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

/**
 * It is just DAO for Entity "DockerImage", not the real docker image repository
 * on the website[docker]
 */
interface DockerImageRepository: JpaRepository<DockerImage, Long> {
    @Transactional(readOnly = true)
    fun findByUserName(input: String): DockerImage?
}