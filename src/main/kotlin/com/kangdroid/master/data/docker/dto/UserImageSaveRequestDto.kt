package com.kangdroid.master.data.docker.dto

import com.kangdroid.master.data.docker.DockerImage

/**
 * UserImageSaveRequestDto
 * Whoa! Long Name!
 *
 * This DTO is used when user try to create single container.
 *
 * Variables:
 * id for db-specific variable,
 * userName for user-ID,
 * userPassword for user-Password,
 * dockerId for saving created container ID
 * computeRegion for determine where user requested container in specific compute region.
 */
class UserImageSaveRequestDto(
        var id: Long = 0,
        var userName: String,
        var userPassword: String,
        var dockerId: String = "",
        var computeRegion: String
) {
    fun toEntity() : DockerImage {
        return DockerImage(
                id = id,
                userName = userName,
                userPassword = userPassword,
                dockerId = dockerId,
                computeRegion = computeRegion
        )
    }
}