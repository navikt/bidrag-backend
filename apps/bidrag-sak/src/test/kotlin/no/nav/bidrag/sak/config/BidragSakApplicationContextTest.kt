package no.nav.bidrag.sak.config

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.sak.BidragSakLocal
import no.nav.bidrag.sak.BidragSakProfiles
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BidragSakLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(BidragSakProfiles.TEST, "mock-oauth", "local")
@EnableMockOAuth2Server
class BidragSakApplicationContextTest {
    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @Test
    fun `alle beans kan opprettes ved oppstart`() {
        val beanFactory = applicationContext.beanFactory
        beanFactory.beanDefinitionNames
            .filter { !beanFactory.getBeanDefinition(it).isAbstract }
            .forEach { navn ->
                withClue("Bean '$navn' kunne ikke opprettes") {
                    runCatching { applicationContext.getBean(navn) }.exceptionOrNull() shouldBe null
                }
            }
    }

    @Test
    fun `AppContext skal finne IdentConsumer under bean-navnet identConsumer`() {
        AppContext.getBean(IdentConsumer::class.java) shouldBe applicationContext.getBean("identConsumer", IdentConsumer::class.java)
    }
}
