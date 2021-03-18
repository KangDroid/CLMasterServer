package com.kangdroid.master.data.user

import org.bson.types.ObjectId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.transaction.annotation.Transactional

/**
 * It is just DAO for Entity "User", Might contains User Data.
 */
interface UserRepository : MongoRepository<User, ObjectId> {
    @Transactional(readOnly = true)
    fun findByUserName(input: String): User?

    @Transactional(readOnly = true)
    fun findByUserPassword(input: String): User?
}