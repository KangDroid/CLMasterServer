package com.kangdroid.master.data.docker

import com.kangdroid.master.data.user.User
import javax.persistence.*

@Entity
class DockerImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = Long.MAX_VALUE,

    @Column(name = "docker_id", length = 500, nullable = false)
    var dockerId: String,

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(length = 500, nullable = false)
    var computeRegion: String,
)