package com.kangdroid.master.data.node

import org.bson.types.ObjectId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.transaction.annotation.Transactional

interface NodeRepository : MongoRepository<Node, ObjectId> {
    @Transactional(readOnly = true)
    fun findByIpAddress(target: String): Node?

    @Transactional(readOnly = true)
    fun findByRegionName(target: String): Node?
}