package no.nav.bidrag.samhandler.persistence.repository

import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.samhandler.persistence.entity.Samhandler
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

interface SamhandlerRepository :
    JpaRepository<Samhandler, Int>,
    JpaSpecificationExecutor<Samhandler> {
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Query("select s from samhandlere s where s.ident = :ident")
    fun findByIdentInNewTransaction(ident: String): Samhandler?

    fun findByIdent(ident: String): Samhandler?

    fun findAllByNavnIgnoreCase(navn: String): List<Samhandler>

    fun findAllByOffentligIdAndErOpphørtIsFalse(offentligId: String): List<Samhandler>

    @Query("SELECT set_config('audit.user_id', :userId, true)")
    fun setAuditUserId(userId: String): Any
}

@Component
@Aspect
class SamhandlerSaveAspect(
    private val samhandlerRepository: SamhandlerRepository,
) {
    @Before("execution(* no.nav.bidrag.samhandler.persistence.repository.SamhandlerRepository.save(..))")
    fun setAuditUserId() {
        samhandlerRepository.setAuditUserId(
            TokenUtils.hentSaksbehandlerIdent() ?: TokenUtils.hentApplikasjonsnavn() ?: "ukjent",
        )
    }
}
