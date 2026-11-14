import { dateToDDMMYYYY, deductDays } from "~/utils/date-utils";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { DataViewTable } from "~/components/DataViewTable";
import {
  BeregnTil,
  NotatMalType,
  NotatVirkningstidspunktBarnDto,
} from "~/types/Api";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.yaml";

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
          <DataViewTable
            labelColWidth={"140px"}
            data={[
              {
                label: t.periodeVedtaketVurderes,
                value: beregningsperiodeVisningsnavn(),
              },
              {
                label: t.beregningsperiode,
                value: `${dateToDDMMYYYY(virkningstidspunkt.virkningstidspunkt as string)} - ${dateToDDMMYYYY(deductDays(virkningstidspunkt.beregnTilDato as string, 1))}`,
              },
            ]}
          />
        </div>
      </div>
    </div>
  );
}
