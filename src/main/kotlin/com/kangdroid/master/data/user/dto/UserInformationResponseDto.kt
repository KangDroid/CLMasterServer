package com.kangdroid.master.data.user.dto

class UserInformationResponseDto(
    var userName: String = "",
    var userRole: Set<String> = setOf()
)