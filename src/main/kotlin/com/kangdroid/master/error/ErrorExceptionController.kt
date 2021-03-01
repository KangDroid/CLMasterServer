package com.kangdroid.master.error

import com.kangdroid.master.error.exception.EmailConflictException
import com.kangdroid.master.error.exception.UnknownErrorException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ErrorExceptionController {
    @ExceptionHandler(EmailConflictException::class)
    fun handleEmailConflict(emailConflictException: EmailConflictException) : ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    HttpStatus.CONFLICT,
                    emailConflictException.message ?: "No Message"
                )
            )
    }

    @ExceptionHandler(UnknownErrorException::class)
    fun handleUnknownException(unknownErrorException: UnknownErrorException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    unknownErrorException.message ?: "No Message"
                )
            )
    }
}