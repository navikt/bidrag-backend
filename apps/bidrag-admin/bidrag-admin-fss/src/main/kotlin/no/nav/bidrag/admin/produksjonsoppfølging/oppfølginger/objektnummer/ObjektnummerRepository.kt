package no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.objektnummer

import no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ObjektnummerRepository : JpaRepository<Rolle, Long> {
    @Query(
        "SELECT DISTINCT r1.saksnr " +
            "  FROM Rolle r1 " +
            "    INNER JOIN Rolle r2 ON r1.saksnr = r2.saksnr " +
            "      AND r1.type = r2.type " +
            "      AND r1.objektnr = r2.objektnr " +
            "      AND r1.fnr < r2.fnr " +
            "  WHERE r1.type = 'BA'",
    )
    fun finnSakerMedGjenbrukteObjektnummer(): MutableList<String?>?
}
