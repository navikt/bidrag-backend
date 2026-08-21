import { hentTekst, SpråkType } from "~/utils/oversettelser";
import { SignaturBoks } from "./SignaturBoks";
import { useId } from "react";

export default function Underskrifter({ språk }: { språk: SpråkType }) {
  const tekster = hentTekst(språk, tekst);
  const headingId = `underskrifter-${useId()}`;

  return (
    <section className="underskrifter" aria-labelledby={headingId}>
      <h2 id={headingId}>{tekster.tittel}</h2>
      <div className="flex flex-col gap-2">
        <SignaturBoks bidragstype="MOTTAKER" språk={språk} />
        <SignaturBoks bidragstype="PLIKTIG" språk={språk} />
      </div>
    </section>
  );
}

const tekst = {
  tittel: {
    nb: "Underskrifter",
    nn: "Underskrifter",
    en: "Signatures",
  },
};
