package com.kangdroid.master.data.user

import com.kangdroid.master.error.exception.NotFoundException
import com.kangdroid.master.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class UserTemplateRepository {
    // Default Logger
    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    fun clearAll() {
        logger.debug("Removing All data in repository.")
        mongoTemplate.remove(Query(), User::class.java)
    }

    fun saveUser(user: User): User {
        logger.debug("Saving user: ${user.userName}")
        return mongoTemplate.save(user)
    }

    fun findByUserName(userName: String): User {
        val findQuery: Query = Query.query(
            Criteria.where("userName").`is`(userName)
        )

        logger.debug("Query: $findQuery")

        // This is nullable
        return runCatching {
            mongoTemplate.findOne(findQuery, User::class.java)
        }.getOrElse {
            logger.error(it.stackTraceToString())
            // Return null
            null
        } ?: throw NotFoundException("Cannot find user: $userName")
    }
}