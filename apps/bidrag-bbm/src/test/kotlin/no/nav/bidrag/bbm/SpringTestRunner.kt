package no.nav.bidrag.bbm

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.EnableTransactionManagement

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [BidragBBMLocal::class])
@SpringBootTest(classes = [BidragBBMLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableMockOAuth2Server
@EnableTransactionManagement
class SpringTestRunner {
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    val rootUri get() = LOCALHOST + port

    protected fun getPort(): String = port.toString()

    companion object {
        private const val LOCALHOST = "http://localhost:"
    }
}
