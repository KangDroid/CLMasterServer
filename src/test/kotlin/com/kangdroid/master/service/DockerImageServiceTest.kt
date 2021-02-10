package com.kangdroid.master.service

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.data.docker.DockerImageRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class DockerImageServiceTest {
    @Autowired
    private lateinit var dockerImageService: DockerImageService

    @Autowired
    private lateinit var dockerImageRepository: DockerImageRepository

    @After
    fun cleanupDb() {
        dockerImageRepository.deleteAll()
    }

    @Test
    fun isSavingWorksTrue() {
        // Let
        val entityTesting: DockerImage = DockerImage(
                userName = "Test",
                userPassword = "Test",
                dockerId = "Test",
                computeRegion = "Test"
        )

        // Assert!
        assertThat(dockerImageService.saveWithCheck(entityTesting)).isEqualTo("")
    }

    @Test
    fun isSavingWorksFalse() {
        // Let
        val entityTesting: DockerImage = DockerImage(
                userName = "Test",
                userPassword = "Test",
                dockerId = "Test",
                computeRegion = "Test"
        )

        // Assert!
        assertThat(dockerImageService.saveWithCheck(entityTesting)).isEqualTo("")

        // Second Saving
        assertThat(dockerImageService.saveWithCheck(entityTesting)).isNotEqualTo("")
    }
}