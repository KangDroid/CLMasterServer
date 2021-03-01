package com.kangdroid.master.data.docker

import org.springframework.data.jpa.repository.JpaRepository

interface DockerImageRepository : JpaRepository<DockerImage, Long>