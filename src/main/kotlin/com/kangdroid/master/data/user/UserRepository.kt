package com.kangdroid.master.data.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * It is just DAO for Entity "User", Might contains User Data.
 */
interface UserRepository : JpaRepository<User, Long> {
    @Transactional(readOnly = true)
    fun findByUserName(input: String): User?

    @Transactional(readOnly = true)
    fun findByUserPassword(input: String): User?

    @Transactional(readOnly = true)
    fun findByUserToken(input: String): User?

    @Transactional(readOnly = true)
    fun findByEmail(email: String): Optional<User>
}