package com.kangdroid.master.data.node

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface NodeRepository: JpaRepository<Node, Long> {
    @Transactional(readOnly = true)
    fun findByIpAddress(target: String): Node?

    @Transactional(readOnly = true)
    fun findByRegionName(target: String): Node?
}