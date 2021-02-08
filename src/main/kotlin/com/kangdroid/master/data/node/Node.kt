package com.kangdroid.master.data.node

import javax.persistence.*

@Entity
class Node(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = Long.MAX_VALUE,

    @Column(length = 500, nullable = false)
    var hostName: String,

    @Column(length = 500, nullable = false)
    var ipAddress: String
)