package com.github.ljufa.sma.backend.db

import kotlinx.coroutines.flow.Flow
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.data.repository.kotlin.CoroutineCrudRepository


@Configuration
class FlywayConfig(private val env: Environment) {
    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(value = ["spring.flyway.enabled"], havingValue = "true",matchIfMissing = true)
    fun flyway(): Flyway? {
        return Flyway(
            Flyway.configure()
                .baselineOnMigrate(true)
                .dataSource(
                    env.getRequiredProperty("spring.flyway.url"),
                    env.getRequiredProperty("spring.flyway.user"),
                    env.getRequiredProperty("spring.flyway.password")
                )
        )
    }
}

interface UserRepository : CoroutineCrudRepository<User, String> {
    fun findByAuthId(authId: String): Flow<User>
}
interface AccountRepository : CoroutineCrudRepository<Account, Long>{
    fun findByUserId(userId: Long): Flow<Account>
}



