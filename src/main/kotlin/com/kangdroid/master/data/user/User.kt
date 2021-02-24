package com.kangdroid.master.data.user

import com.kangdroid.master.data.docker.DockerImage
import org.springframework.security.core.userdetails.UserDetails
import javax.persistence.*
import java.util.stream.Collectors

import org.springframework.security.core.authority.SimpleGrantedAuthority

import org.springframework.security.core.GrantedAuthority

import java.util.ArrayList

import javax.persistence.FetchType

import javax.persistence.ElementCollection




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

    @Column(length = 500, nullable = false, unique = true)
    var email: String = "",

    @ElementCollection(fetch = FetchType.EAGER)
    val roles: Set<String> = setOf(),

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user", cascade = [CascadeType.ALL])
    var dockerImage: MutableList<DockerImage> = mutableListOf()
): UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority?>? {
        return roles.stream()
            .map { role: String? ->
                SimpleGrantedAuthority(
                    role
                )
            }
            .collect(Collectors.toList())
    }

    override fun getPassword(): String {
        return userPassword
    }

    override fun getUsername(): String? {
        return email
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}