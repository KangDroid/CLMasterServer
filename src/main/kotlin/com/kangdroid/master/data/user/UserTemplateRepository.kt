package com.kangdroid.master.data.user

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.error.exception.NotFoundException
import com.kangdroid.master.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class UserTemplateRepository {
    private val userIdField: String = "id"
    private val userNameField: String = "userName"
    private val dockerImageField: String = "dockerImage"
    private val fileListTokenField: String = "token"
    private val fileListPrevTokenField: String = "prevToken"

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
        return mongoTemplate.findOne(findQuery, User::class.java) ?: run {
            throw NotFoundException("Cannot find user: $userName")
        }
    }

    fun findDockerImageByContainerID(userName: String, containerId: String): DockerImage {
        // Match user name [Filter username first]
        val userNameMatchCriteria: Criteria = Criteria.where(userNameField).`is`(userName)
        val matchOperation: MatchOperation = Aggregation.match(userNameMatchCriteria)

        // Unwind
        val unwindOperation: UnwindOperation = Aggregation.unwind(dockerImageField)

        // Match Token [Filter token]
        val fileTokenMatchCriteria: Criteria = Criteria.where("dockerImage.dockerId").`is`(containerId)
        val fileTokenMatchOperation: MatchOperation = Aggregation.match(fileTokenMatchCriteria)

        // Group
        val groupOperation: GroupOperation = Aggregation.group(userIdField)
            .push(
                dockerImageField
            ).`as`(dockerImageField)

        val userAggregationResult: AggregationResults<User> = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                    matchOperation,
                    unwindOperation,
                    fileTokenMatchOperation,
                    groupOperation
                ),
                User::class.java,
                User::class.java
            )

        if (userAggregationResult.mappedResults.size != 1) {
            throw NotFoundException("Cannot find docker image corresponding $containerId for user $userName")
        }

        if (userAggregationResult.mappedResults[0].dockerImage.size != 1) {
            throw NotFoundException("Cannot find docker image corresponding $containerId for user $userName")
        }

        return userAggregationResult.mappedResults[0].dockerImage[0]
    }
}