package com.kangdroid.master.data.user

import com.kangdroid.master.data.docker.DockerImage
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.stream.Collectors
import javax.persistence.*

@Document(collection="user")
class User(
    @Id
    var id: ObjectId = ObjectId(),
    var userName: String = "",
    var userPassword: String = "",
    val roles: Set<String> = setOf(),
    var dockerImage: MutableList<DockerImage> = mutableListOf()
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority?>? {
        return roles.stream()
            .map { role: String? ->
                SimpleGrantedAuthority(
                    role
                )
            }
            .collect(Collectors.toList())
    }

    override fun getPassword() = userPassword
    override fun getUsername(): String? = userName
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}