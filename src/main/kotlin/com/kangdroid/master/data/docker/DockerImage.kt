package com.kangdroid.master.data.docker

import javax.persistence.*

@Entity
class DockerImage(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = Long.MAX_VALUE,

        @Column(length = 500, nullable = false)
        var userName: String,

        @Column(length = 500, nullable = false)
        var userPassword: String,

        @Column(length = 500, nullable = false)
        var dockerId: String,

        @Column(length = 500, nullable = false)
        var computeRegion: String
)