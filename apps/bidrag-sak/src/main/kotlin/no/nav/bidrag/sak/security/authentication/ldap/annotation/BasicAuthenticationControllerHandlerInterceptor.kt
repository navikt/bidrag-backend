package no.nav.bidrag.sak.security.authentication.ldap.annotation

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.bidrag.sak.security.SecurityUtils
import no.nav.bidrag.sak.security.authentication.ldap.LdapUserService
import no.nav.bidrag.sak.security.exception.BasicNotAuthenticatedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

class BasicAuthenticationControllerHandlerInterceptor(
    private val ldapUserService: LdapUserService,
) : HandlerInterceptor {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private enum class AuthenticationMethod {
        Basic,
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler is HandlerMethod) {
            val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
            val protectedWithBasic = getProtectedWithBasicAnnotation(handler)

            return if (protectedWithBasic != null) {
                if (authHeader != null &&
                    authHeader.contains(AuthenticationMethod.Basic.toString()) &&
                    handleProtectedWithBasicAnnotation(authHeader, protectedWithBasic.groups)
                ) {
                    logger.debug("Basic authentication succeeded!")
                    true
                } else {
                    throw BasicNotAuthenticatedException(
                        "Endpoint is protected with Basic authentication, however, " +
                            "correct credentials are not provided.",
                    )
                }

                // Check for OIDC protection
            } else {
                super.preHandle(request, response, handler)
            }
        }
        return true
    }

    private fun getProtectedWithBasicAnnotation(handlerMethod: HandlerMethod): ProtectedWithBasic? {
        val methodBasicAnnotation = handlerMethod.getMethodAnnotation(ProtectedWithBasic::class.java)
        return if (methodBasicAnnotation != null) {
            logger.debug("method $handlerMethod marked @ProtectedWithBasic")
            methodBasicAnnotation
        } else {
            val method = handlerMethod.method
            val declaringClass = method.declaringClass
            val classBasicAnnotation = declaringClass.getAnnotation(ProtectedWithBasic::class.java)
            if (classBasicAnnotation != null) {
                logger.debug("Class $declaringClass marked @ProtectedWithBasic")
            }
            classBasicAnnotation
        }
    }

    @Throws(BasicNotAuthenticatedException::class)
    private fun handleProtectedWithBasicAnnotation(
        authHeader: String,
        groups: Array<String>,
    ): Boolean {
        val userIsAuthenticated: Boolean
        val loginCredentials = SecurityUtils.extractUsernameAndPasswordFromBasicAuthHeader(authHeader)
        return try {
            userIsAuthenticated =
                ldapUserService.authenticate(loginCredentials[0], loginCredentials[1], listOf(*groups))
            logger.debug(
                "User {} was successfully authenticated: {}",
                loginCredentials[0],
                userIsAuthenticated,
            )
            userIsAuthenticated
        } catch (e: Exception) {
            logger.warn("An error occurred when looking up user {} in AD", loginCredentials[0], e)
            throw BasicNotAuthenticatedException("Basic user authentication failed")
        }
    }
}
