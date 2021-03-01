package com.kangdroid.master.error

import com.kangdroid.master.error.exception.ConflictException
import com.kangdroid.master.error.exception.ForbiddenException
import com.kangdroid.master.error.exception.NotFoundException
import com.kangdroid.master.error.exception.UnknownErrorException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ErrorExceptionController {
    private val no_message: String = "No Message"
    @ExceptionHandler(ConflictException::class)
    fun handleConflict(conflictException: ConflictException) : ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    HttpStatus.CONFLICT,
                    conflictException.message ?: no_message
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
                    unknownErrorException.message ?: no_message
                )
            )
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(forbiddenException: ForbiddenException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse(
                    HttpStatus.FORBIDDEN,
                    forbiddenException.message ?: no_message
                )
            )
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(notFoundException: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    HttpStatus.NOT_FOUND,
                    notFoundException.message ?: no_message
                )
            )
    }
}