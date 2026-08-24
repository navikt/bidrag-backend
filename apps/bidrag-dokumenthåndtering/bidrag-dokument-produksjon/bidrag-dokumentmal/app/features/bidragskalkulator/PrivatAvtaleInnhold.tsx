/* eslint-disable prettier/prettier */
import { hentTekst, jaNeiTekster, SpråkType } from "~/utils/oversettelser";
import Innholdsseksjon from "./Innholdsseksjon";
import Underskrifter from "./Underskrifter";
import {
  Bidragstype,
  IPerson,
  bidragstypeTekster,
  IBarnOgBidrag,
  IBidrag,
  IOppgjør,
  oppgjørsformTekster,
  IAndreBestemmelser,
  Vedleggskrav,
  vedleggskravTekster,
  GenererPrivatAvtalePdfRequest,
} from "~/types/bidragskalkulator";
import { formatterBeløpMedSpråk } from "~/utils/visningsnavn";
import { formatterÅrMåned } from "~/utils/date-utils";

type Props = {
  språk: SpråkType;
  data: GenererPrivatAvtalePdfRequest;
};

export default function PrivatAvtaleInnhold({ språk, data }: Props) {
  const innhold = hentTekst(språk, innholdsseksjonTekst);
  let barnOgBidrag: IBarnOgBidrag[] | null = null;
  let bidrag: IBidrag[] | null = null;

  const { privatAvtalePdf, navSkjemaId } = data;

  if (navSkjemaId === "AVTALE_OM_BARNEBIDRAG_UNDER_18") {
    barnOgBidrag = privatAvtalePdf.barn;
  } else {
    bidrag = privatAvtalePdf.bidrag;
  }

  return (
    <div className="flex flex-col gap-4">
      <Innholdsseksjon
        tekst={innhold.opplysningerPerson(
          "MOTTAKER",
          privatAvtalePdf.bidragsmottaker,
          true
        )}
      />
      <Innholdsseksjon
        tekst={innhold.opplysningerPerson(
          "PLIKTIG",
          privatAvtalePdf.bidragspliktig
        )}
      />
      {barnOgBidrag && (
        <Innholdsseksjon tekst={innhold.barnOgBidrag(barnOgBidrag)} />
      )}
      {bidrag && (
        <Innholdsseksjon tekst={innhold.opplysningerOmBidrag(bidrag)} />
      )}
      <Innholdsseksjon tekst={innhold.oppgjør(privatAvtalePdf.oppgjør)} />
      <Innholdsseksjon
        tekst={innhold.andreBestemmelser(privatAvtalePdf.andreBestemmelser)}
      />
      <Innholdsseksjon tekst={innhold.vedlegg(privatAvtalePdf.vedlegg)} />
      <Underskrifter språk={språk} />
    </div>
  );
}

const innholdsseksjonTekst = {
  opplysningerPerson: (
    bidragstype: Bidragstype,
    person: IPerson,
    erOver18: boolean = false
  ) => {
    const rolle = bidragstypeTekster[bidragstype];
    return {
      nb: {
        overskrift: `Opplysninger om ${rolle.nb.toLowerCase()} ${erOver18 ? "(barnet over 18 år)" : ""}`,
        innhold: [
          { label: "Fornavn", value: person.fornavn, vis: true, type: "text" },
          {
            label: "Etternavn",
            value: person.etternavn,
            vis: true,
            type: "text",
          },
          ...(person.ident
            ? [
                {
                  label: "Fødselsnummer eller D-nummer (11 siffer)",
                  value: person.ident,
                  vis: true,
                  type: "text",
                },
              ]
            : []),
        ],
      },
      nn: {
        overskrift: `Opplysningar om ${rolle.nn.toLowerCase()} ${erOver18 ? "(barnet over 18 år)" : ""}`,
        innhold: [
          { label: "Fornamn", value: person.fornavn, vis: true, type: "text" },
          {
            label: "Etternamn",
            value: person.etternavn,
            vis: true,
            type: "text",
          },
          ...(person.ident
            ? [
                {
                  label: "Fødselsnummer eller D-nummer (11 siffer)",
                  value: person.ident,
                  vis: true,
                  type: "text",
                },
              ]
            : []),
        ],
      },
      en: {
        overskrift: `Information about ${rolle.en.toLowerCase()} ${erOver18 ? "(child over 18 years)" : ""}`,
        innhold: [
          {
            label: "First name",
            value: person.fornavn,
            vis: true,
            type: "text",
          },
          {
            label: "Last name",
            value: person.etternavn,
            vis: true,
            type: "text",
          },
          ...(person.ident
            ? [
                {
                  label:
                    "Norwegian national identification number or D-number (11 digits)",
                  value: person.ident,
                  vis: true,
                  type: "text",
                },
              ]
            : []),
        ],
      },
    };
  },
  barnOgBidrag: (barn: IBarnOgBidrag[]) => ({
    nb: {
      overskrift: "Opplysninger om barn og bidrag",
      innhold: barn.flatMap((b, i) => [
        {
          label: `Barn ${i + 1}`,
          value: "",
          vis: barn.length > 1,
          type: "heading",
        },
        { label: "Fornavn", value: b.fornavn, vis: true, type: "text" },
        { label: "Etternavn", value: b.etternavn, vis: true, type: "text" },
        {
          label: "Fødselsnummer eller D-nummer (11 siffer)",
          value: b.ident,
          vis: true,
          type: "text",
        },
        {
          label: "Beløp per måned",
          value: formatterBeløpMedSpråk(b.sumBidrag),
          vis: true,
          type: "text",
        },
        {
          label: "Avtalen gjelder fra (dd.mm.åååå)",
          value: b.fraDato,
          vis: true,
          type: "text",
        },
      ]),
    },
    nn: {
      overskrift: "Opplysningar om barn og bidrag",
      innhold: barn.flatMap((b, i) => [
        {
          label: `Barn ${i + 1}`,
          value: "",
          vis: barn.length > 1,
          type: "heading",
        },
        { label: "Fornamn", value: b.fornavn, vis: true, type: "text" },
        { label: "Etternamn", value: b.etternavn, vis: true, type: "text" },
        {
          label: "Fødselsnummer eller D-nummer (11 siffer)",
          value: b.ident,
          vis: true,
          type: "text",
        },
        {
          label: "Beløp per månad",
          value: formatterBeløpMedSpråk(b.sumBidrag),
          vis: true,
          type: "text",
        },
        {
          label: "Avtalen gjeld frå (dd.mm.åååå)",
          value: b.fraDato,
          vis: true,
          type: "text",
        },
      ]),
    },
    en: {
      overskrift: "Information about child and support",
      innhold: barn.flatMap((b, i) => [
        {
          label: `Child ${i + 1}`,
          value: "",
          vis: barn.length > 1,
          type: "heading",
        },
        { label: "First name", value: b.fornavn, vis: true, type: "text" },
        { label: "Last name", value: b.etternavn, vis: true, type: "text" },
        {
          label: "Norwegian national identification number / D number",
          value: b.ident,
          vis: true,
          type: "text",
        },
        {
          label: "Child support amount",
          value: formatterBeløpMedSpråk(b.sumBidrag, "en"),
          vis: true,
          type: "text",
        },
        {
          label: "Agreement valid from (dd.mm.yyyy)",
          value: b.fraDato,
          vis: true,
          type: "text",
        },
      ]),
    },
  }),
  opplysningerOmBidrag: (bidrag: IBidrag[]) => ({
    nb: {
      overskrift: "Opplysninger om bidrag",
      innhold: bidrag.flatMap((b, i) => [
        {
          label: `Periode ${i + 1}`,
          value: "",
          vis: bidrag.length > 1,
          type: "heading",
        },
        {
          label: "Bidrag per måned",
          value: formatterBeløpMedSpråk(b.bidragPerMåned),
          vis: true,
          type: "text",
        },
        {
          label: "Bidraget skal betales f.o.m.",
          value: formatterÅrMåned(b.fraDato),
          vis: true,
          type: "text",
        },
        {
          label: "Bidraget skal betales t.o.m.",
          value: formatterÅrMåned(b.tilDato),
          vis: true,
          type: "text",
        },
      ]),
    },
    nn: {
      overskrift: "Opplysningar om bidrag",
      innhold: bidrag.flatMap((b, i) => [
        {
          label: `Periode ${i + 1}`,
          value: "",
          vis: bidrag.length > 1,
          type: "heading",
        },
        {
          label: "Bidrag per månad",
          value: formatterBeløpMedSpråk(b.bidragPerMåned),
          vis: true,
          type: "text",
        },
        {
          label: "Bidraget skal betalast f.o.m.",
          value: formatterÅrMåned(b.fraDato),
          vis: true,
          type: "text",
        },
        {
          label: "Bidraget skal betalast t.o.m.",
          value: formatterÅrMåned(b.tilDato),
          vis: true,
          type: "text",
        },
      ]),
    },
    en: {
      overskrift: "Information about the support",
      innhold: bidrag.flatMap((b, i) => [
        {
          label: `Period ${i + 1}`,
          value: "",
          vis: bidrag.length > 1,
          type: "heading",
        },
        {
          label: "Support per month",
          value: formatterBeløpMedSpråk(b.bidragPerMåned, "en"),
          vis: true,
          type: "text",
        },
        {
          label: "Support is to be paid from",
          value: formatterÅrMåned(b.fraDato, "en"),
          vis: true,
          type: "text",
        },
        {
          label: "Support is to be paid until",
          value: formatterÅrMåned(b.tilDato, "en"),
          vis: true,
          type: "text",
        },
      ]),
    },
  }),
  oppgjør: (oppgjør: IOppgjør) => ({
    nb: {
      overskrift: "Oppgjør",
      innhold: [
        {
          label: "Er dette en ny avtale?",
          value: oppgjør.nyAvtale
            ? "Ja"
            : "Nei, dette er en endring av en eksisterende avtale",
          vis: true,
          type: "text",
        },
        {
          label: "Hvilken oppgjørsform ønskes?",
          value: oppgjørsformTekster[oppgjør.oppgjørsformØnsket].nb,
          vis: true,
          type: "text",
        },
      ],
    },
    nn: {
      overskrift: "Oppgjer",
      innhold: [
        {
          label: "Er dette ein ny avtale?",
          value: oppgjør.nyAvtale
            ? "Ja"
            : "Nei, det er ei endring av eksisterande avtale.",
          vis: true,
          type: "text",
        },
        {
          label: "Kva type oppgjer ønskjer de?",
          value: oppgjørsformTekster[oppgjør.oppgjørsformØnsket].nn,
          vis: true,
          type: "text",
        },
      ],
    },
    en: {
      overskrift: "Settlement",
      innhold: [
        {
          label: "Is this a new agreement?",
          value: oppgjør.nyAvtale
            ? "Yes"
            : "No, this is an adjustment of an existing agreement.",
          vis: true,
          type: "text",
        },
        {
          label: "Which settlement method is desired?",
          value: oppgjørsformTekster[oppgjør.oppgjørsformØnsket].en,
          vis: true,
          type: "text",
        },
      ],
    },
  }),
  andreBestemmelser: (andreBestemmelser: IAndreBestemmelser) => ({
    nb: {
      overskrift: "Andre bestemmelser",
      innhold: [
        {
          label: "Er det andre bestemmelser tilknyttet avtalen?",
          value: andreBestemmelser.harAndreBestemmelser
            ? jaNeiTekster.JA.nb
            : jaNeiTekster.NEI.nb,
          vis: true,
          type: "text",
        },
        {
          label: "Andre bestemmelser tilknyttet avtalen",
          value: andreBestemmelser.beskrivelse,
          vis:
            (andreBestemmelser.harAndreBestemmelser &&
              andreBestemmelser.beskrivelse) ??
            false,
          type: "text",
        },
      ],
    },
    nn: {
      overskrift: "Andre bestemmingar",
      innhold: [
        {
          label: "Er det andre bestemmingar knytt til avtalen?",
          value: andreBestemmelser.harAndreBestemmelser
            ? jaNeiTekster.JA.nn
            : jaNeiTekster.NEI.nn,
          vis: true,
          type: "text",
        },
        {
          label: "Andre bestemmingar knytt til avtalen",
          value: andreBestemmelser.harAndreBestemmelser
            ? jaNeiTekster.JA.nn
            : jaNeiTekster.NEI.nn,
          vis:
            (andreBestemmelser.harAndreBestemmelser &&
              andreBestemmelser.beskrivelse) ??
            false,
          type: "text",
        },
      ],
    },
    en: {
      overskrift: "Other provisions",
      innhold: [
        {
          label: "Are there any other conditions that apply to the agreement?",
          value: andreBestemmelser.harAndreBestemmelser
            ? jaNeiTekster.JA.en
            : jaNeiTekster.NEI.en,
          vis: true,
          type: "text",
        },
        {
          label: "Other conditions related to the agreement",
          value: andreBestemmelser.harAndreBestemmelser,
          vis:
            (andreBestemmelser.harAndreBestemmelser &&
              andreBestemmelser.beskrivelse) ??
            false,
          type: "text",
        },
      ],
    },
  }),
  vedlegg: (vedlegg: Vedleggskrav) => ({
    nb: {
      overskrift: "Vedlegg",
      innhold: [
        {
          label: "Har du noen annen dokumentasjon du ønsker å legge ved?",
          value: vedleggskravTekster[vedlegg].nb,
          vis: true,
          type: "text",
        },
      ],
    },
    nn: {
      overskrift: "Vedlegg",
      innhold: [
        {
          label: "Annan dokumentasjon",
          value: vedleggskravTekster[vedlegg].nn,
          vis: true,
          type: "text",
        },
      ],
    },
    en: {
      overskrift: "Attachments",
      innhold: [
        {
          label: "Other documentation",
          value: vedleggskravTekster[vedlegg].en,
          vis: true,
          type: "text",
        },
      ],
    },
  }),
};

export type GeneriskInnholdType = ReturnType<
  (typeof innholdsseksjonTekst)[keyof typeof innholdsseksjonTekst]
>[SpråkType];
