package com.kangdroid.master.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class ErrorResponseTest {
    @Test
    fun isCreatingWithRightResponse() {
        // With Internal Server Error
        val errorClass: ErrorResponse = ErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorMessage = "Testing Error Message"
        )

        assertThat(errorClass.errorMessage).isEqualTo("Testing Error Message")
        assertThat(errorClass.statusMessage).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase)
        assertThat(errorClass.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value().toString())
    }
}