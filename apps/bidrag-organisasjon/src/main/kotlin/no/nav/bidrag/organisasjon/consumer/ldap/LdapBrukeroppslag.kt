package no.nav.bidrag.organisasjon.consumer.ldap

import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import javax.naming.LimitExceededException
import javax.naming.NamingException
import javax.naming.directory.Attribute
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.LdapContext

class LdapBrukeroppslag(
    private val environment: Map<String, Any?>,
    private val ldapInnlogging: LdapInnlogging,
    private val searchBase: String?,
) {
    fun hentBrukernavn(ident: String): String? {
        if (ident.isEmpty()) {
            LOGGER.warn("Saksbehandler ident mangler")
            return null
        }
        val context = ldapInnlogging.lagLdapContext(environment)
        if (context == null || searchBase == null) {
            return null
        }
        val result = ldapSearch(ident, context) ?: return null
        return getDisplayName(result)
    }

    private fun ldapSearch(ident: String, context: LdapContext): SearchResult? {
        val matcher = IDENT_PATTERN.matcher(ident)
        if (!matcher.matches()) {
            LOGGER.warn("Navn på saksbehandler for ident $ident ikke funnet (1)")
            return null
        }
        val controls = SearchControls()
        controls.searchScope = SearchControls.SUBTREE_SCOPE
        controls.countLimit = 1
        val soekestreng = String.format("(cn=%s)", ident)
        return try {
            val result = context.search(searchBase, soekestreng, controls)
            if (result.hasMoreElements()) {
                return result.nextElement()
            }
            LOGGER.warn("Navn på saksbehandler for ident $ident ikke funnet (2)")
            null
        } catch (lee: LimitExceededException) {
            LOGGER.warn("Navn på saksbehandler for ident $ident ikke funnet (LimitExceededException)")
            null
        } catch (ne: NamingException) {
            LOGGER.warn("Navn på saksbehandler for ident $ident ikke funnet (NamingException)")
            null
        }
    }

    fun getDisplayName(result: SearchResult): String? {
        val displayName = find(result) ?: return null
        return try {
            displayName.get().toString()
        } catch (e: NamingException) {
            LOGGER.warn("Navn på saksbehandler ikke funnet (NamingException)")
            null
        }
    }

    private fun find(element: SearchResult): Attribute? {
        val attributeName = "displayName"
        val attribute = element.attributes[attributeName]
        if (attribute == null) {
            LOGGER.warn("Navn på saksbehandler ikke funnet (attribute == null)")
            return null
        }
        return attribute
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LdapBrukeroppslag::class.java)
        private val IDENT_PATTERN = Pattern.compile("^\\p{LD}+$")
    }
}
