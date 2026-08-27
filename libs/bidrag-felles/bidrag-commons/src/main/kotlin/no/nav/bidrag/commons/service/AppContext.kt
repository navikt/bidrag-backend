package no.nav.bidrag.commons.service

import org.springframework.beans.BeansException
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
class AppContext : ApplicationContextAware {
    @Throws(BeansException::class)
    override fun setApplicationContext(argApplicationContext: ApplicationContext) {
        applicationContext = argApplicationContext
    }

    companion object {
        var applicationContext: ApplicationContext? = null
            private set

        fun <T> getBean(clazz: Class<T>): T = applicationContext!!.getBean(clazz.simpleName.replaceFirstChar { it.lowercase() }, clazz) as T

        fun <T> getBean(
            name: String,
            clazz: Class<T>,
        ): T = applicationContext!!.getBean(name, clazz) as T
    }
}
