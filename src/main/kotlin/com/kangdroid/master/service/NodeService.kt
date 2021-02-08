package com.kangdroid.master.service

import com.kangdroid.master.data.node.Node
import com.kangdroid.master.data.node.NodeRepository
import com.kangdroid.master.data.node.dto.NodeSaveRequestDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NodeService {
    @Autowired
    private lateinit var nodeRepository: NodeRepository

    fun save(nodeSaveRequestDto: NodeSaveRequestDto): String {
        val node: Node = nodeSaveRequestDto.toEntity()
        node.regionName = "Region-${nodeRepository.count()}"
        return nodeRepository.save(node).regionName
    }
}