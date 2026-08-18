package no.nav.bidrag.regnskap.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    val duration = 60L

    @Bean
    fun caffeineConfig() = CaffeineCacheManager().apply {
        setCaffeine(Caffeine.newBuilder().expireAfterWrite(duration, TimeUnit.SECONDS))
    }
}
