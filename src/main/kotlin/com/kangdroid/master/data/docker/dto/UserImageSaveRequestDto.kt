package com.kangdroid.master.data.docker.dto

import com.kangdroid.master.data.docker.DockerImage

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