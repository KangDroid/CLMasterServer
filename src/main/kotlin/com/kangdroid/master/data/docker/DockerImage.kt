package com.kangdroid.master.data.docker

import javax.persistence.*

@Entity
@Table(name = "TESTING")
class DockerImage(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = Long.MAX_VALUE,

        @Column(name = "docker_id", length = 500, nullable = false)
        var dockerId: String,

        @Column(length = 500, nullable = false)
        var computeRegion: String,
)