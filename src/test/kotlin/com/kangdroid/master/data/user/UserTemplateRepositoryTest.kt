package com.kangdroid.master.data.user

import com.kangdroid.master.data.docker.DockerImage
import com.kangdroid.master.error.exception.NotFoundException
import com.kangdroid.master.service.UserService
import junit.framework.Assert.fail
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest
@RunWith(SpringRunner::class)
class UserTemplateRepositoryTest {
    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    // Default Logger
    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    @After
    @Before
    fun clearAllRepository() {
        userTemplateRepository.clearAll()
    }

    @Test
    fun is_findByUserName_throws_NotFoundException_nouser() {
        runCatching  {
            userTemplateRepository.findByUserName("non-exist")
        }.onSuccess {
            fail("There is no such username called non-exist")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_findByUserName_works_well() {
        val mockUser: User = User(
            userName = "KangDroid",
            userPassword = "testPassword",
        )
        userTemplateRepository.saveUser(
            mockUser
        )

        runCatching {
            userTemplateRepository.findByUserName(mockUser.userName)
        }.onFailure {
            logger.error(it.stackTraceToString())
            fail("Username should exists!")
        }.onSuccess {
            assertThat(it.userName).isEqualTo(mockUser.userName)
        }
    }

    @Test
    fun is_findDockerImageByContainerId_returns_error_no_name() {
        runCatching {
            userTemplateRepository.findDockerImageByContainerID("no_name", "wrong_token")
        }.onSuccess {
            fail("User is not registered, but somehow this function passed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).contains("Cannot find docker image corresponding")
        }
    }

    @Test
    fun is_findDockerImageByContainerId_returns_error_wrong_id() {
        val mockUser: User = User(
            userName = "KangDroid",
            userPassword = "testPassword",
        )
        userTemplateRepository.saveUser(
            mockUser
        )

        runCatching {
            userTemplateRepository.findDockerImageByContainerID("KangDroid", "wrong_token")
        }.onSuccess {
            fail("User is registered, but should be no container out there. But somehow this function passed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).contains("Cannot find docker image corresponding")
        }
    }

    @Test
    fun is_findDockerImageByContainerId_success() {
        val mockUser: User = User(
            userName = "KangDroid",
            userPassword = "testPassword",
            dockerImage = mutableListOf(
                DockerImage(
                    dockerId = "test_id",
                    computeRegion = "Region-0"
                )
            )
        )
        userTemplateRepository.saveUser(
            mockUser
        )

        runCatching {
            userTemplateRepository.findDockerImageByContainerID("KangDroid", "test_id")
        }.onSuccess {
            assertThat(it.dockerId).isEqualTo("test_id")
            assertThat(it.computeRegion).isEqualTo("Region-0")
        }.onFailure {
            println(it.message)
            fail("User, Docker container is registered but it failed somehow")
        }
    }
}