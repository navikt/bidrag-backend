package no.nav.bidrag.organisasjon.consumer.ldap

import org.slf4j.LoggerFactory
import java.util.Hashtable
import javax.naming.NamingException
import javax.naming.ldap.InitialLdapContext
import javax.naming.ldap.LdapContext

class LdapInnlogging {
    fun lagLdapContext(environment: Map<String, Any?>): LdapContext? = try {
        InitialLdapContext(Hashtable(environment), null)
    } catch (e: NamingException) {
        LOGGER.warn("Navn på saksbehandler ikke funnet (NamingException)")
        null
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LdapInnlogging::class.java)
    }
}
