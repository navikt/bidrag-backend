import felles from "./felles.json";

// Tekstene ligger i disse filene:
// - ord.json: ord og korte setninger som brukes flere steder, f.eks. Ja, Nei, Årsak og Periode.
// - felles.json: kolonnenavn og begreper som brukes på tvers av malene.
// - notat/<notat>/saksbehandler.json: tittel og delene saksbehandleren har vurdert, i samme rekkefølge som i notatet.
// - notat/<notat>/opplysninger.json: vedleggene med opplysninger og beregningsdetaljer.
//   <notat> er felles, forskudd, særbidrag eller bidrag. "felles" er deler som er like i alle notatene.
export default felles;
