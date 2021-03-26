package com.kangdroid.master.data.node

import com.kangdroid.master.data.user.User
import com.kangdroid.master.error.exception.NotFoundException
import com.kangdroid.master.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class NodeTemplateRepository {
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    // Default Logger
    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    private val ipAddressField: String = "ipAddress"
    private val regionField: String = "regionName"

    fun clearAll() {
        logger.debug("Removing All data in repository.")
        mongoTemplate.remove(Query(), Node::class.java)
    }

    fun saveNode(node: Node): Node {
        return mongoTemplate.save(node)
    }

    fun findAll(containerSort: Boolean = false): List<Node> {
        val retList: List<Node> = mongoTemplate.find(Query(), Node::class.java)
        return if (containerSort) {
            retList.sortedBy {it.containerList.size}
            retList
        } else {
            retList
        }
    }

    fun count(): Long {
        return mongoTemplate.count(Query(), Node::class.java)
    }

    fun findNodeByIpAddress(nodeIpAddress: String): Node {
        val findQuery: Query = Query.query(Criteria.where(ipAddressField).`is`(nodeIpAddress))
        return runCatching {
            mongoTemplate.findOne(findQuery, Node::class.java)
        }.onFailure {
            logger.error("Cannot find with ip address: $nodeIpAddress")
            logger.error(it.stackTraceToString())
        }.getOrNull() ?: throw NotFoundException("Cannot find node with ip address: $nodeIpAddress")
    }

    fun findNodeByRegionName(nodeRegionName: String): Node {
        val findQuery: Query = Query.query(Criteria.where(regionField).`is`(nodeRegionName))
        return runCatching {
            mongoTemplate.findOne(findQuery, Node::class.java)
        }.onFailure {
            logger.error("Cannot find with region: $nodeRegionName")
            logger.error(it.stackTraceToString())
        }.getOrNull() ?: throw NotFoundException("Cannot find node with region: $nodeRegionName")
    }
}