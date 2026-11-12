import felles from "~/tekster/felles.yaml";

// Tekstene ligger i disse filene:
// - ord.yaml: ord og korte setninger som brukes flere steder, f.eks. Ja, Nei, Årsak og Periode.
// - felles.yaml: kolonnenavn og begreper som brukes på tvers av malene.
// - notat/<notat>/saksbehandler.yaml: tittel og delene saksbehandleren har vurdert, i samme rekkefølge som i notatet.
// - notat/<notat>/opplysninger.yaml: vedleggene med opplysninger og beregningsdetaljer.
//   <notat> er felles, forskudd, særbidrag eller bidrag. "felles" er deler som er like i alle notatene.
export default felles;
