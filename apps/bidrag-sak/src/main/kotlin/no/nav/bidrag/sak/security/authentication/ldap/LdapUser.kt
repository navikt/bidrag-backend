package no.nav.bidrag.sak.security.authentication.ldap

import org.springframework.ldap.odm.annotations.Attribute
import org.springframework.ldap.odm.annotations.DnAttribute
import org.springframework.ldap.odm.annotations.Entry
import org.springframework.ldap.odm.annotations.Id
import javax.naming.Name

@Entry(objectClasses = ["user"])
class LdapUser(
    @Id
    var id: Name,
    @Attribute(name = "cn")
    @DnAttribute(value = "cn")
    var username: String,
    @Attribute(name = "userPrincipalName")
    var userPrincipalName: String,
    @Attribute(name = "memberOf")
    var memberOf: List<String>,
    @Attribute(name = "password")
    var password: String,
) {
    val memberOfString get() = memberOf.joinToString(",")

    override fun toString(): String = username
}
