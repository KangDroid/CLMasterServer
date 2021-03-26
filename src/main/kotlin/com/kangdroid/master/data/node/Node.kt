package com.kangdroid.master.data.node

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import javax.persistence.*

@Document(collection = "node")
class Node(
    @Id
    var id: ObjectId = ObjectId.get(),
    var hostName: String = "",
    var ipAddress: String = "",
    var hostPort: String = "",
    var regionName: String = "",
    var containerList: MutableList<String> = mutableListOf()
)