package com.kangdroid.master.data.docker.dto

class UserImageSaveRequestDto(
        var id: Long = 0,
        var userName: String,
        var userPassword: String,
        var dockerId: String = "",
        var computeRegion: String
)