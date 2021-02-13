package com.kangdroid.master.data.docker

import javax.persistence.*

@Embeddable
class DockerImage(
        var dockerId: String,
        var computeRegion: String,
)