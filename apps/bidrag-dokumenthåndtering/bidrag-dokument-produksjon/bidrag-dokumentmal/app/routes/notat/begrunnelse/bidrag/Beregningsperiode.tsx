import { dateToDDMMYYYY, deductDays } from "~/utils/date-utils";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import {
  BeregnTil,
  NotatMalType,
  NotatVirkningstidspunktBarnDto,
} from "~/types/Api";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

const tekst = bidragOpplysningerTekster.beregningsdetaljer;

const t = tekst.beregningsperiode;

export default function Beregningsperiode({
  virkningstidspunkt,
}: {
  virkningstidspunkt: NotatVirkningstidspunktBarnDto;
}) {
  const { data, type } = useNotatFelles();
  const skalViseBeregningsperiode =
    type == NotatMalType.BIDRAG && data.erOrkestrertVedtak;
  if (!skalViseBeregningsperiode) return null;
  function beregningsperiodeVisningsnavn() {
    switch (virkningstidspunkt.beregnTil) {
      case BeregnTil.ETTERFOLGENDEMANUELLVEDTAK:
        return t.beregnTil.etterfølgendeManueltVedtak.replace(
          "{}",
          `${dateToDDMMYYYY(virkningstidspunkt.etterfølgendeVedtakVirkningstidspunkt as string)}`,
        );
      case BeregnTil.INNEVAeRENDEMANED:
        return t.beregnTil.inneværendeMåned;
      case BeregnTil.OPPRINNELIG_VEDTAKSTIDSPUNKT:
        return t.beregnTil.opprinneligVedtakstidspunkt;
    }
  }
  return (
    <div className={"virkningstidspunkt pt-1"}>
      <div>
        <div className={"flex flex-row justify-between w-[500px]"}>
          <div>
            <FeltTabell labelColWidth={"140px"}>
              <Feltrad label={t.periodeVedtaketVurderes}>
                {beregningsperiodeVisningsnavn()}
              </Feltrad>
              <Feltrad
                label={t.beregningsperiode}
              >{`${dateToDDMMYYYY(virkningstidspunkt.virkningstidspunkt as string)} - ${dateToDDMMYYYY(deductDays(virkningstidspunkt.beregnTilDato as string, 1))}`}</Feltrad>
            </FeltTabell>
          </div>
        </div>
      </div>
    </div>
  );
}
