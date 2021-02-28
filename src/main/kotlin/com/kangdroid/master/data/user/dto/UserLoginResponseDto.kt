package com.kangdroid.master.data.user.dto

import com.kangdroid.master.error.Response

class UserLoginResponseDto(
    var token: String = "",
    var errorMessage: String = ""
): Response