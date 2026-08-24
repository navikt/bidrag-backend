import { useId } from "react";
import { GeneriskInnholdType } from "~/routes/bidragskalkulator.privatavtale/route";

interface IProps {
  tekst: GeneriskInnholdType;
}

export default function Innholdsseksjon({ tekst }: IProps) {
  const sectionId = useId();
  const headingId = `h-${sectionId}`;

  return (
    <section aria-labelledby={headingId}>
      <h2 id={headingId}>{tekst.overskrift}</h2>
      <dl className="flex flex-col gap-3">
        {tekst.innhold.map((linje, index) => {
          if (!linje.vis) return null;

          if (linje.vis && linje.type === "heading") {
            return <h3 key={`h-${index}`}>{linje.label}</h3>;
          }

          return (
            <div key={index} className="flex flex-col">
              <dt>{linje.label}</dt>
              <dd>{linje.value}</dd>
            </div>
          );
        })}
      </dl>
    </section>
  );
}
