package com.kangdroid.master.error

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus

class ErrorResponse(
    @JsonIgnore
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val errorMessage: String = ""
): Response() {
    var statusCode: String = httpStatus.value().toString()
    var statusMessage: String = httpStatus.reasonPhrase
}