import elementIds from "~/utils/elementIds";
import {
  BidragPeriodeBeregningsdetaljer,
  DokumentmalResultatBidragsberegningBarnDto,
  ResultatBarnebidragsberegningPeriodeDto,
  Resultatkode,
  Rolletype,
  Vedtakstype,
} from "~/types/Api";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { DataViewTable } from "~/components/DataViewTable";
import { deductDays, formatPeriode } from "~/utils/date-utils";
import HorizontalLine from "~/components/HorizontalLine";
import { BPAndelUnderholdskostnad } from "~/routes/notat.bidrag/beregningsdetaljer/BPAndelUnderholdskostnad";
import { Samværsfradrag } from "~/routes/notat.bidrag/beregningsdetaljer/Samværsfradrag";
import { NettoBarnetilleggTable } from "~/routes/notat.bidrag/beregningsdetaljer/NettoBarnetilleggTable";
import { BeregningFordeltBidragEvne } from "~/routes/notat.bidrag/beregningsdetaljer/BeregningFordeltBidragEvne";
import { createContext, useContext } from "react";
import { EndeligBidragTable } from "~/routes/notat.bidrag/beregningsdetaljer/EndeligBidrag";
import { BPsEvneTable } from "~/components/vedtak/BPsEvneTable";
import { BarnetilleggSkattesats } from "~/routes/notat.bidrag/beregningsdetaljer/BarnetilleggSkattesats";
import { VedleggProps } from "~/types/commonTypes";
import { BeregningBegrensetRevurdering } from "~/routes/notat.bidrag/beregningsdetaljer/BeregningBegrensetRevurdering";
import { EndringUnderGrense } from "~/routes/notat.bidrag/beregningsdetaljer/EndringUnderGrense";
import { AldersjusteringDetaljer } from "~/routes/notat.bidrag/beregningsdetaljer/AldersjusteringDetaljer";
import { IndeksreguleringDetaljer } from "~/routes/notat.bidrag/beregningsdetaljer/IndeksreguleringDetaljer";
import { BeregningForholdsmessigFordeling } from "~/routes/notat.bidrag/beregningsdetaljer/BeregningForholdsmessigFordelingDetaljer";
import { PersonV2 } from "~/components/Person";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";

export default function VedleggBeregningsDetaljer({
  vedleggNummer,
}: VedleggProps) {
  const { erAvslag, data, erInnkrevingsgrunnlag } = useNotatFelles();
  if (erAvslag || erInnkrevingsgrunnlag) return null;
  return (
    <>
      <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
        <h2 id={elementIds.vedleggBeregningsdetaljer}>
          Vedlegg nr. {vedleggNummer}: Beregningsdetaljer
        </h2>
        <VedleggBeregningsDetaljerInnhold />
        {data.erOrkestrertVedtak && (
          <div>
            <h2 id={elementIds.vedleggBeregningsdetaljer}>
              Beregningsdetaljer endelig vedtak
            </h2>
            <VedleggBeregningsDetaljerEndeligVedtakInnhold />
          </div>
        )}
      </div>
    </>
  );
}

type BeregnindDetaljerContextProps = BidragPeriodeBeregningsdetaljer & {
  endeligBeløp: number;
  kanFatteVedtakForRevurderingsbarn: boolean;
  periode: ResultatBarnebidragsberegningPeriodeDto;
  erEndringUnderGrense: boolean;
};
export const BidragBeregningContext =
  createContext<BeregnindDetaljerContextProps | null>(null);

export function useBeregningDetaljer(): Required<BeregnindDetaljerContextProps> {
  return useContext(
    BidragBeregningContext,
  ) as Required<BeregnindDetaljerContextProps>;
}

function VedleggBeregningsDetaljerEndeligVedtakInnhold() {
  const { data } = useNotatFelles();
  const resultatPerioder = data.vedtak
    ?.resultat as DokumentmalResultatBidragsberegningBarnDto[];
  const perioder = resultatPerioder.map((d) =>
    d.orkestrertVedtak?.perioder.filter(
      (d) =>
        d.vedtakstype == Vedtakstype.INDEKSREGULERING ||
        d.vedtakstype == Vedtakstype.ALDERSJUSTERING,
    ),
  );

  if (
    perioder.every((p) => p?.filter((pd) => !pd.erDirekteAvslag)?.length === 0)
  ) {
    return <div>Ingen beregningsdetaljer å vise</div>;
  }

  return (
    <>
      {resultatPerioder?.map(
        (resultat: DokumentmalResultatBidragsberegningBarnDto) => {
          return (
            <>
              <DataViewTable
                data={[
                  {
                    label: rolleTilVisningsnavnV2(resultat.barn),
                    labelBold: true,
                    value: (
                      <PersonV2 {...resultat.barn} visFødselsdato={false} />
                    ),
                  },
                ]}
              />
              {resultat.orkestrertVedtak?.perioder.length === 0 ? (
                <div>Ingen beregningsdetaljer å vise</div>
              ) : (
                resultat.orkestrertVedtak?.perioder
                  .filter(
                    (d) =>
                      d.vedtakstype == Vedtakstype.INDEKSREGULERING ||
                      d.vedtakstype == Vedtakstype.ALDERSJUSTERING,
                  )
                  .map((periode) => {
                    const detaljer = periode.beregningsdetaljer!;

                    function renderTables() {
                      if (periode.vedtakstype == Vedtakstype.ALDERSJUSTERING) {
                        return (
                          <>
                            <AldersjusteringDetaljer />
                            <EndeligBidragTable />
                          </>
                        );
                      }
                      if (periode.vedtakstype == Vedtakstype.INDEKSREGULERING) {
                        return <IndeksreguleringDetaljer />;
                      }
                    }
                    return (
                      <>
                        <BidragBeregningContext.Provider
                          value={{
                            ...detaljer,
                            periode,
                            kanFatteVedtakForRevurderingsbarn:
                              data.vedtak.kanFatteVedtakForRevurderingsbarn,
                            endeligBeløp: periode.faktiskBidrag,
                            erEndringUnderGrense:
                              periode.resultatKode ===
                              Resultatkode.INGEN_ENDRING_UNDER_GRENSE,
                          }}
                        >
                          <>
                            <div className={"pb-2"}>
                              <DataViewTable
                                className={"mb-2"}
                                data={[
                                  {
                                    label: "Periode",
                                    labelBold: true,
                                    value: formatPeriode(
                                      periode.periode.fom,
                                      deductDays(periode.periode!.til, 1),
                                    ),
                                  },
                                ]}
                              />
                              {renderTables()}
                            </div>
                          </>
                        </BidragBeregningContext.Provider>
                        <HorizontalLine />
                      </>
                    );
                  })
              )}
            </>
          );
        },
      )}
    </>
  );
}
function VedleggBeregningsDetaljerInnhold() {
  const { data } = useNotatFelles();
  const resultatPerioder = data.vedtak
    ?.resultat as DokumentmalResultatBidragsberegningBarnDto[];
  if (resultatPerioder.length == 0) return <div>Mangler resultat</div>;

  const erForholdsmessigFordelt = resultatPerioder.some(
    (b) => b.minstEnPeriodeHarSlåttUtTilFF,
  );
  const enPeriodeHarSlåttUtTilFFPgaRedusertEvne = resultatPerioder.some(
    (b) =>
      b.minstEnPeriodeHarSlåttUtTilFF &&
      b.perioderSlåttUtTilFF.some(
        (b) => !b.erEvneJustertNedTil25ProsentAvInntekt,
      ),
  );
  const enPeriodeHarSlåttUtTilFFPga25Prosent = resultatPerioder.some(
    (b) =>
      b.minstEnPeriodeHarSlåttUtTilFF &&
      b.perioderSlåttUtTilFF.some(
        (b) => b.erEvneJustertNedTil25ProsentAvInntekt,
      ),
  );
  return (
    <>
      {erForholdsmessigFordelt && (
        <div className={"pb-2"}>
          <h4>Forholdsmessig fordeling</h4>
          {enPeriodeHarSlåttUtTilFFPga25Prosent &&
            "BPs totale andel av U overstiger 25% av inntekten. Bidraget er derfor forholdsmessig fordelt i minst en av periodene"}
          {enPeriodeHarSlåttUtTilFFPgaRedusertEvne &&
            "BP har ikke full bidragsevne. Bidraget er derfor forholdsmessig fordelt i minst en av periodene"}
        </div>
      )}
      {resultatPerioder?.map(
        (resultat: DokumentmalResultatBidragsberegningBarnDto) => {
          const perioderUtenAvslag = resultat.perioder.filter(
            (p) => !p.erDirekteAvslag,
          );
          return (
            <>
              <DataViewTable
                data={[
                  {
                    label: rolleTilVisningsnavnV2(resultat.barn),
                    labelBold: true,
                    value: resultat.barn.navn,
                  },
                ]}
              />
              {perioderUtenAvslag.length === 0 ? (
                <div>Ingen beregningsdetaljer å vise</div>
              ) : (
                perioderUtenAvslag
                  .filter(
                    (d) =>
                      d.beregningsdetaljer?.sluttberegning &&
                      !d.beregningsdetaljer.sluttberegning!
                        .barnetErSelvforsørget &&
                      !d.beregningsdetaljer.sluttberegning!.ikkeOmsorgForBarnet,
                  )
                  .map((periode) => {
                    const detaljer = periode.beregningsdetaljer!;
                    return (
                      <>
                        <BidragBeregningContext.Provider
                          value={{
                            ...detaljer,
                            periode,
                            endeligBeløp: periode.faktiskBidrag,
                            kanFatteVedtakForRevurderingsbarn:
                              data.vedtak.kanFatteVedtakForRevurderingsbarn,
                            erEndringUnderGrense:
                              periode.resultatKode ===
                              Resultatkode.INGEN_ENDRING_UNDER_GRENSE,
                          }}
                        >
                          <>
                            <div className={"pb-2"}>
                              <DataViewTable
                                className={"mb-2"}
                                data={[
                                  {
                                    label: "Periode",
                                    labelBold: true,
                                    value: formatPeriode(
                                      periode.periode.fom,
                                      deductDays(periode.periode!.til, 1),
                                    ),
                                  },
                                ]}
                              />
                              <BPAndelUnderholdskostnad />
                            </div>
                            {!detaljer.deltBosted && (
                              <>
                                <Samværsfradrag />
                                <div className={"pt-2 pb-2"} />
                              </>
                            )}
                            {!detaljer.deltBosted &&
                              detaljer.barnetilleggBM &&
                              detaljer.barnetilleggBM.barnetillegg.length >
                                0 && (
                                <>
                                  <BarnetilleggSkattesats
                                    rolle={Rolletype.BM}
                                  />
                                  <NettoBarnetilleggTable
                                    rolle={Rolletype.BM}
                                  />
                                  <div className={"pt-2 pb-2"} />
                                </>
                              )}
                            <div className={"pt-2 pb-2"}>
                              <BPsEvneTable
                                inntekter={detaljer!.inntekter!}
                                delberegningBidragsevne={
                                  detaljer!.delberegningBidragsevne!
                                }
                              />
                              <BeregningFordeltBidragEvne />
                              <BeregningForholdsmessigFordeling />
                            </div>

                            <BeregningBegrensetRevurdering />
                            {!detaljer.deltBosted &&
                              detaljer.barnetilleggBP?.barnetillegg &&
                              detaljer.barnetilleggBP?.barnetillegg?.length >
                                0 && (
                                <>
                                  <BarnetilleggSkattesats
                                    rolle={Rolletype.BP}
                                  />
                                  <NettoBarnetilleggTable
                                    rolle={Rolletype.BP}
                                  />
                                  <div className={"pt-2 pb-2"} />
                                </>
                              )}
                            <EndeligBidragTable />
                            <EndringUnderGrense />
                          </>
                        </BidragBeregningContext.Provider>
                        <HorizontalLine />
                      </>
                    );
                  })
              )}
            </>
          );
        },
      )}
    </>
  );
}
