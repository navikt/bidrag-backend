package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.ForespørselDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.MotpartDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.PersonDto;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.database.datamodell.Barn;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.BidragPersonkonsument;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Diskresjonskode;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Familiemedlem;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentFamilieRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.MotpartBarnRelasjon;
import no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Mapper {

    private ForespørselDao forespørselDao;
    private BidragPersonkonsument bidragPersonkonsument;

    @Autowired
    public Mapper(BidragPersonkonsument bidragPersonkonsument, ForespørselDao forespørselDao) {
        this.bidragPersonkonsument = bidragPersonkonsument;
        this.forespørselDao = forespørselDao;
    }

    public BrukerinformasjonDto tilDto(HentFamilieRespons familieRespons) {
        var forespørslerHvorPersonErHovedpart = forespørselDao.henteSynligeForespørslerForHovedpart(familieRespons.getPerson().getIdent(),
                henteGrenseForSisteEndring());
        var forespørslerHvorPersonErMotpart = forespørselDao.henteSynligeForespørslerForMotpart(familieRespons.getPerson().getIdent(),
                henteGrenseForSisteEndring());

        var hovedpersonHarDiskresjon = Diskresjonskode.harDiskresjon(familieRespons.getPerson());
        var familierUtenDiskresjon = henteMotpartBarnRelasjonerSomIkkeHarDiskresjon(familieRespons);
        var familierUtenDiskresjonDødEllerUkjentMotpart = familierUtenDiskresjon.stream().filter(Objects::nonNull)
                .filter(m -> erIkkeDød(m.getMotpart())).filter(this::motpartErKjent).collect(Collectors.toSet());

        return BrukerinformasjonDto.builder()
                .fornavn(familieRespons.getPerson().getFornavn())
                .harDiskresjon(hovedpersonHarDiskresjon)
                .kjønn(familieRespons.getPerson().getKjoenn())
                .harSkjulteFamilieenheterMedDiskresjon(
                        familierUtenDiskresjon.size() < familieRespons.getPersonensMotpartBarnRelasjon().size())
                .kanSøkeOmFordelingAvReisekostnader(!hovedpersonHarDiskresjon && personHarDeltForeldreansvar(familierUtenDiskresjonDødEllerUkjentMotpart))
                .barnMinstFemtenÅr(hovedpersonHarDiskresjon ? new HashSet<>() : henteBarnOverFemtenÅrMedKjentMotpart(familierUtenDiskresjonDødEllerUkjentMotpart))
                .forespørslerSomHovedpart(tilForespørselDto(forespørslerHvorPersonErHovedpart))
                .forespørslerSomMotpart(tilForespørselDto(forespørslerHvorPersonErMotpart))
                .motparterMedFellesBarnUnderFemtenÅr(
                        hovedpersonHarDiskresjon ? new HashSet<>() : filtrereUtMotparterMedFellesBarnUnderFemtenÅr(familierUtenDiskresjonDødEllerUkjentMotpart))
                .build();
    }

    private LocalDateTime henteGrenseForSisteEndring() {
        return LocalDate.now().minusDays(Applikasjonskonfig.FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 1).atStartOfDay();
    }

    private PersonDto tilDto(Familiemedlem familiemedlem) {
        var ident = familiemedlem.getIdent() == null ? null : kryptere(familiemedlem.getIdent());
        return new PersonDto(ident, familiemedlem.getFornavn(), null, familiemedlem.getFoedselsdato());
    }

    /**
     * Filtrerer bort alle familieenehter hvor enten motpart eller minst ett av barna har diskresjon
     */
    private Set<MotpartBarnRelasjon> henteMotpartBarnRelasjonerSomIkkeHarDiskresjon(HentFamilieRespons familierespons) {
        var motpartBarnRelasjonUtenMotparterMedDiskresjon = filtrereBortEnheterDerMotpartHarDiskresjon(familierespons.getPersonensMotpartBarnRelasjon());

        return motpartBarnRelasjonUtenMotparterMedDiskresjon.stream().filter(Objects::nonNull)
                .filter(m -> !Diskresjonskode.harMinstEttFamiliemedlemHarDiskresjon(m.getFellesBarn())).collect(Collectors.toSet());
    }

    private Set<MotpartBarnRelasjon> filtrereBortEnheterDerMotpartHarDiskresjon(List<MotpartBarnRelasjon> motpartBarnRelasjons) {
        return motpartBarnRelasjons.stream().filter(Objects::nonNull).filter(m -> m.getMotpart() != null && StringUtils.isEmpty(m.getMotpart().getDiskresjonskode()))
                .collect(Collectors.toSet());
    }

    private boolean personHarDeltForeldreansvar(Set<MotpartBarnRelasjon> motpartBarnRelasjoner) {
        return motpartBarnRelasjoner.size() > 0 && motpartBarnRelasjoner.iterator().hasNext() && harMinstEnKjentMotpart(motpartBarnRelasjoner)
                && motpartBarnRelasjoner.iterator().next().getFellesBarn().size() > 0;
    }

    private boolean harMinstEnKjentMotpart(Set<MotpartBarnRelasjon> motpartBarnRelasjoner) {
        return motpartBarnRelasjoner.stream().filter(Objects::nonNull).filter(this::motpartErKjent).collect(Collectors.toList()).size() > 0;
    }

    private boolean motpartErKjent(MotpartBarnRelasjon motpartBarnRelasjon) {
        var motpartHarPersonident  = motpartBarnRelasjon.getMotpart() != null && !StringUtils.isEmpty(motpartBarnRelasjon.getMotpart().getIdent());
        if (motpartHarPersonident) {
            return true;
        } else {
            log.warn("Ukjent motpart med relasjon {} til felles barn", motpartBarnRelasjon.getRelasjonMotpart());
            SIKKER_LOGG.warn("Ukjent motpart med relasjon {} til felles barn med identer: {}", motpartBarnRelasjon.getRelasjonMotpart(), motpartBarnRelasjon.getFellesBarn().stream().map(f-> f.getIdent()).collect(Collectors.toList()));
            return false;
        }
    }

    private Set<PersonDto> henteBarnOverFemtenÅrMedKjentMotpart(Set<MotpartBarnRelasjon> motpartBarnRelasjoner) {
        return motpartBarnRelasjoner.stream().filter(Objects::nonNull).flatMap(mbr -> mbr.getFellesBarn().stream()).filter(this::erMellomFemtenOgAttenÅr)
                .filter(this::erIkkeDød)
                .map(this::tilDto).collect(Collectors.toSet());
    }

    private boolean erIkkeDød(Familiemedlem familiemedlem) {
        return familiemedlem.getDoedsdato() == null || LocalDate.now().isBefore(familiemedlem.getDoedsdato());
    }

    private boolean erMellomFemtenOgAttenÅr(Familiemedlem barn) {

        if (barn == null || barn.getFoedselsdato() == null) {
            return false;
        }

        return barn.getFoedselsdato().isBefore(LocalDate.now().plusDays(1).minusYears(15))
                && barn.getFoedselsdato().isAfter(LocalDate.now().minusYears(18));
    }

    private boolean erUnderFemtenÅr(Familiemedlem barn) {

        if (barn == null || barn.getFoedselsdato() == null) {
            return false;
        }

        return barn.getFoedselsdato().isAfter(LocalDate.now().minusYears(15));
    }

    private Set<MotpartDto> filtrereUtMotparterMedFellesBarnUnderFemtenÅr(Set<MotpartBarnRelasjon> motpartBarnRelasjoner) {
        Set<MotpartDto> motparterMedBarnUnderFemtenÅr = new HashSet<>();
        for (MotpartBarnRelasjon motpartBarnRelasjon : motpartBarnRelasjoner.stream().collect(Collectors.toSet())) {
            var barnUnderFemtenÅr = filtereUtBarnUnderFemtenÅr(motpartBarnRelasjon.getFellesBarn());
            if (barnUnderFemtenÅr.size() > 0) {
                var motpartDto = MotpartDto.builder().motpart(tilDto(motpartBarnRelasjon.getMotpart())).fellesBarnUnder15År(barnUnderFemtenÅr).build();
                motparterMedBarnUnderFemtenÅr.add(motpartDto);
            }
        }
        return motparterMedBarnUnderFemtenÅr;
    }

    private Set<PersonDto> filtereUtBarnUnderFemtenÅr(List<Familiemedlem> barn) {
        return barn.stream().filter(Objects::nonNull).filter(this::erUnderFemtenÅr).filter(this::erIkkeDød).map(this::tilDto).collect(Collectors.toSet());
    }

    private Set<ForespørselDto> tilForespørselDto(Set<Forespørsel> forespørsler) {

        for (Forespørsel f : forespørsler) {
            var t = tilForespørselDto(f);
            assert (true);
        }

        return forespørsler.stream().filter(Objects::nonNull).map(this::tilForespørselDto).collect(Collectors.toSet());
    }

    private ForespørselDto tilForespørselDto(Forespørsel forespørsel) {
        return ForespørselDto.builder()
                .id(forespørsel.getId())
                .kreverSamtykke(forespørsel.isKreverSamtykke())
                .barn(tilPersonDtoSetFraBarn(forespørsel.getBarn()))
                .hovedpart(tilPersonDto(forespørsel.getHovedpart()))
                .motpart(tilPersonDto(forespørsel.getMotpart()))
                .opprettet(forespørsel.getOpprettet())
                .samtykket(forespørsel.getSamtykket())
                .samtykkefrist(forespørsel.getSamtykkefrist())
                .journalført(forespørsel.getJournalført())
                .deaktivert(forespørsel.getDeaktivert())
                .deaktivertAv(forespørsel.getDeaktivertAv())
                .build();
    }

    private PersonDto tilPersonDto(Forelder forelder) {
        return forelder == null ? null : tilPersonDto(forelder.getPersonident());
    }

    private Set<PersonDto> tilPersonDtoSetFraBarn(Set<Barn> barn) {
        return barn.stream().filter(Objects::nonNull).map(b -> tilPersonDto(b.getPersonident())).collect(Collectors.toSet());
    }

    public Set<Barn> tilEntitet(Set<String> personidenterBarn) {
        return personidenterBarn.stream().filter(Objects::nonNull).filter(s -> !s.isEmpty())
                .map(personident -> Barn.builder().personident(personident).fødselsdato(hentBarnFødselsdato(personident)).build()).collect(Collectors.toSet());
    }

    public PersonDto tilPersonDto(String personident) {
        var personinfo = bidragPersonkonsument.hentPersoninfo(personident);
        return new PersonDto(kryptere(personident), personinfo.getFornavn(), personinfo.getKortnavn(), personinfo.getFoedselsdato());
    }

    private LocalDate hentBarnFødselsdato(String personIdent) {
        var barn = bidragPersonkonsument.hentPersoninfo(personIdent);
        return barn.getFoedselsdato();
    }

    private String kryptere(String ukryptertPersonident) {
        return Krypteringsverktøy.kryptere(ukryptertPersonident);
    }
}
