package no.nav.bidrag.samhandler.persistence.repository

import no.nav.bidrag.samhandler.persistence.entity.Samhandler
import no.nav.bidrag.transport.samhandler.SamhandlerSøk
import org.springframework.data.jpa.domain.Specification

object SamhandlerSøkSpec {
    fun søkPåAlleParameter(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification
        .where(navnLike(samhandlerSøk))
        .and(identEquals(samhandlerSøk))
        .and(offentligIdEquals(samhandlerSøk))
        .and(postnummerEquals(samhandlerSøk))
        .and(poststedEquals(samhandlerSøk))
        .and(kontonummerEquals(samhandlerSøk))
        .and(ibanEquals(samhandlerSøk))
        .and(swiftEquals(samhandlerSøk))
        .and(banknavnEquals(samhandlerSøk))
        .and(banklandkodeEquals(samhandlerSøk))
        .and(bankcodeEquals(samhandlerSøk))
        .and(erIkkeOpphørt())

    private fun navnLike(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.navn.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.like(
                criteriaBuilder.upper(root.get("navn")),
                "%" + samhandlerSøk.navn!!.uppercase() + "%",
            )
        }
    }

    private fun identEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.ident.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("ident")),
                samhandlerSøk.ident!!.uppercase(),
            )
        }
    }

    private fun offentligIdEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.offentligId.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("offentligId")),
                samhandlerSøk.offentligId!!.uppercase(),
            )
        }
    }

    private fun postnummerEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.postnummer.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("postnummer")),
                samhandlerSøk.postnummer!!.uppercase(),
            )
        }
    }

    private fun poststedEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.poststed.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("poststed")),
                samhandlerSøk.poststed!!.uppercase(),
            )
        }
    }

    private fun kontonummerEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.norskkontonr.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("norskkontonr")),
                samhandlerSøk.norskkontonr!!.uppercase(),
            )
        }
    }

    private fun ibanEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.iban.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("iban")),
                samhandlerSøk.iban!!.uppercase(),
            )
        }
    }

    private fun swiftEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.swift.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("swift")),
                samhandlerSøk.swift!!.uppercase(),
            )
        }
    }

    private fun banknavnEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.banknavn.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("banknavn")),
                samhandlerSøk.banknavn!!.uppercase(),
            )
        }
    }

    private fun banklandkodeEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.banklandkode.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("banklandkode")),
                samhandlerSøk.banklandkode!!.uppercase(),
            )
        }
    }

    private fun bankcodeEquals(samhandlerSøk: SamhandlerSøk): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        if (samhandlerSøk.bankcode.isNullOrEmpty()) {
            criteriaBuilder.conjunction()
        } else {
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("bankcode")),
                samhandlerSøk.bankcode!!.uppercase(),
            )
        }
    }

    private fun erIkkeOpphørt(): Specification<Samhandler> = Specification<Samhandler> { root, _, criteriaBuilder ->
        criteriaBuilder.isFalse(root.get("erOpphørt"))
    }
}
