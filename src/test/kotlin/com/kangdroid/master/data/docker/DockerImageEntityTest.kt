package com.kangdroid.master.data.docker

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class DockerImageEntityTest {

    @Autowired
    private lateinit var dockerImageRepository: DockerImageRepository

    @After
    fun clearData() = dockerImageRepository.deleteAll()

    @Test
    fun isSaveWorks() {
        // Pre Assert
        assertThat(dockerImageRepository.count()).isEqualTo(0L)

        // Let
        val dockerImageEntity: DockerImage = DockerImage (
                id = 20,
                userName = "kangdroid",
                userPassword = "testPassword",
                dockerId = "d82..",
                computeRegion = "Region-1"
        )

        // save
        dockerImageRepository.save(dockerImageEntity)

        // Post Assert
        assertThat(dockerImageRepository.count()).isEqualTo(1L)
    }
}