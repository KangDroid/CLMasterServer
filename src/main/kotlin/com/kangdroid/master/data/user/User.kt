package com.kangdroid.master.data.user

import com.kangdroid.master.data.docker.DockerImage
import javax.persistence.*

@Entity
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = Long.MAX_VALUE,

    @Column(length = 500, nullable = false)
    var userName: String,

    @Column(length = 500, nullable = false)
    var userPassword: String,

    @Column(length = 500, nullable = true)
    var userToken: String = "",

    @Column(length = 500, nullable = true)
    var userTokenExp: Long = 0,

    @OneToMany(fetch=FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "docker_id")
    var dockerImage: MutableList<DockerImage>? = null
)