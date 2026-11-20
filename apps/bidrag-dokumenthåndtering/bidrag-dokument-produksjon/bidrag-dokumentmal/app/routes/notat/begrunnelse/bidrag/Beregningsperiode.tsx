import { dateToDDMMYYYY, deductDays } from "~/utils/date-utils";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import {
  BeregnTil,
  NotatMalType,
  NotatVirkningstidspunktBarnDto,
} from "~/types/Api";
import { tekst } from "~/utils/tekst";
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
        return tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.beregningsperiode.beregnTil.etterfølgendeManueltVedtak",
          "Til etterfølgende vedtak med virkningstidspunkt {}",
        ).replace(
          "{}",
          `${dateToDDMMYYYY(virkningstidspunkt.etterfølgendeVedtakVirkningstidspunkt as string)}`,
        );
      case BeregnTil.INNEVAeRENDEMANED:
        return tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.beregningsperiode.beregnTil.inneværendeMåned",
          "Ut inneværende måned",
        );
      case BeregnTil.OPPRINNELIG_VEDTAKSTIDSPUNKT:
        return tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.beregningsperiode.beregnTil.opprinneligVedtakstidspunkt",
          "Ut måneden opprinnelig vedtak ble fattet",
        );
    }
  }
  return (
    <div className={"virkningstidspunkt pt-1"}>
      <div>
        <div className={"flex flex-row justify-between w-[500px]"}>
          <div>
            <FeltTabell labelColWidth={"140px"}>
              <Feltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.beregningsperiode.periodeVedtaketVurderes",
                  "Periode vedtaket vurderes",
                )}
              >
                {beregningsperiodeVisningsnavn()}
              </Feltrad>
              <Feltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.beregningsperiode.beregningsperiode",
                  "Beregningsperiode",
                )}
              >{`${dateToDDMMYYYY(virkningstidspunkt.virkningstidspunkt as string)} - ${dateToDDMMYYYY(deductDays(virkningstidspunkt.beregnTilDato as string, 1))}`}</Feltrad>
            </FeltTabell>
          </div>
        </div>
      </div>
    </div>
  );
}
