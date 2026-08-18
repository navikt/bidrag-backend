package no.nav.bidrag.sjablon.service;

import java.util.List;
import no.nav.bidrag.sjablon.entity.Sjablontall;
import no.nav.bidrag.sjablon.mapping.CurrentSjablonFilter;
import no.nav.bidrag.sjablon.mapping.SjablontallMapper;
import no.nav.bidrag.sjablon.yaml.SjablonerYaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SjablontallService {

  @Autowired private SjablonerYaml sjabloner;

  public List<Sjablontall> getAllSjablontall() {
    return new SjablontallMapper()
        .addBeløp("0001", sjabloner.getStønader().getBarnetrygd())
        .addBeløp("0002", sjabloner.getStønader().getSmåbarnstillegg())
        .addBeløp("0003", sjabloner.getBidragsberegning().getBoutgifterBidragsbarn())
        .addBeløp("0004", sjabloner.getSkatt().getFordelSkatteklasse2())
        .addBeløp("0005", sjabloner.getForskudd().getForskudd(), p -> p.getSatsForhøyetForskudd())
        .addBeløp("0006", sjabloner.getBidragsberegning().getKapitalinntektInnslag())
        .addBeløp(
            "0007",
            sjabloner.getTilleggsbidragYaml().getTilleggsbidrag(),
            p -> p.getInntektsinterval())
        .addProsent(
            "0008",
            sjabloner.getBidragsberegning().getBidragspliktigsInntekt(),
            p -> p.getMaksAndel())
        .addMultiplikator(
            "0009",
            sjabloner.getBidragsberegning().getBidragspliktigsInntekt(),
            p -> p.getHøyInntekt())
        .addMultiplikator(
            "0010", sjabloner.getBidragsberegning().getBidragsbarnsInntekt(), p -> p.getInnslag())
        .addMultiplikator("0011", sjabloner.getBidragsberegning().getMultiplikatorMaksBidrag())
        .addMultiplikator(
            "0012",
            sjabloner.getBidragsberegning().getBidragsbarnsInntekt(),
            p -> p.getMaksInntekt())
        .addMultiplikator("0013", sjabloner.getForskudd().getMultiplikatorØvreInntektsgrense())
        .addBeløp("0014", sjabloner.getGebyr().getNedreInntektsgrense())
        .addProsent(
            "0015", sjabloner.getSkatt().getSkattAlminneligInntektForBeregningAvBarnetilsyn())
        .addProsent(
            "0016", sjabloner.getTilleggsbidragYaml().getTilleggsbidrag(), p -> p.getProsentsats())
        .addProsent("0017", sjabloner.getSkatt().getTrygdeavgift())
        .addProsent("0018", sjabloner.getStønader().getSkatteprosentBarnetillegg())
        .addBeløp(
            "0019",
            sjabloner.getBidragsevne().getBidragspliktigsKostnader(),
            b -> b.getUnderholdEgneBarn())
        .addProsent("0020", sjabloner.getBidragsberegning().getEndringsgrense())
        .addBeløp(
            "0021", sjabloner.getStønader().getForsvaretsBarnetillegg(), p -> p.getFørsteBarn())
        .addBeløp(
            "0022", sjabloner.getStønader().getForsvaretsBarnetillegg(), p -> p.getØvrigeBarn())
        .addBeløp("0023", sjabloner.getSkatt().getMinstefradrag().getØvreGrense())
        .add("0024", sjabloner.getBidragsberegning().getVirkedagerIMåneden(), p -> p.getVerdi())
        .addProsent("0025", sjabloner.getSkatt().getMinstefradrag().getAndelAvInntekt())
        .addBeløp("0026", sjabloner.getStønader().getDagligSatsBarnetillegg())
        .addBeløp("0027", sjabloner.getSkatt().getPersonfradrag(), p -> p.getBeløpKlasse1())
        .addBeløp("0028", sjabloner.getSkatt().getPersonfradrag(), p -> p.getBeløpKlasse2())
        .addBeløp("0029", sjabloner.getStønader().getKontantstøtte())
        .addBeløp(
            "0030",
            sjabloner.getSærfradrag().getFordelSærfradrag(),
            p -> p.getInntektsgrenser().getHalvFordelFra())
        .addBeløp(
            "0031",
            sjabloner.getSærfradrag().getFordelSærfradrag(),
            p -> p.getInntektsgrenser().getFullFordelFra())
        .addBeløp("0032", sjabloner.getStønader().getEkstraSmåbarnstillegg())
        .addBeløp(
            "0033",
            sjabloner.getForskudd().getForskudd(),
            p -> p.getInntektsgrenser().getForhøyetForskudd())
        .addBeløp(
            "0034",
            sjabloner.getForskudd().getForskudd(),
            p -> p.getInntektsgrenser().getOrdinærtForskuddEnslig())
        .addBeløp(
            "0035",
            sjabloner.getForskudd().getForskudd(),
            p -> p.getInntektsgrenser().getOrdinærtForskuddGiftSamboer())
        .addBeløp(
            "0036",
            sjabloner.getForskudd().getForskudd(),
            p -> p.getInntektsgrenser().getInntektsinterval())
        .addBeløp("0037", sjabloner.getSærbidrag().getØvreGrense())
        .add("0038", sjabloner.getForskudd().getForskudd(), p -> p.getBeregnetOrdinærtForskudd())
        .addBeløp("0039", sjabloner.getSærfradrag().getFordelSærfradrag(), p -> p.getFordelsbeløp())
        .addProsent("0040", sjabloner.getSkatt().getSkattAlminneligInntekt())
        .addBeløp("0041", sjabloner.getStønader().getForhøyetBarnetrygd())
        .addBeløp("0042", sjabloner.getStønader().getUtvidetBarnetrygd())
        .addProsent("0050", sjabloner.getIndeksregulering().getLand().get("NOR").getIndeksprosent())
        .addBeløp("0100", sjabloner.getGebyr().getFastsettelsesgebyr())
        .getListe();
  }

  public List<Sjablontall> getCurrentSjablontall() {
    return getAllSjablontall().stream()
        .filter(new CurrentSjablonFilter<>(Sjablontall::getDatoFom, Sjablontall::getDatoTom))
        .toList();
  }
}
