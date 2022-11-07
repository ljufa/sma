package com.github.ljufa.sma.backend.db

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * DB classes
 */
@Table("twitter.global_rule")
data class GlobalRule(val id: String, val tag: String, val rule: String)


@Table("sma.user")
data class User(
    @Id
    val id: Long? = null,
    val authId: String,
    val createdDate: Instant? = null,
    val enabled: Boolean = true,

    )

enum class ExternalAccountType {
    TWITTER, FACEBOOK, INSTAGRAM,
    REDDIT, TIKTOK, LINKEDIN, GITHUB
}

@Table("sma.account")
data class Account(
    @Id
    val id: Long? = null,
    val userId: Long,
    val externalAccountId: String,
    val externalAccountType: ExternalAccountType,
    val createdDate: Instant? = null,
    val enabled: Boolean = true
)

