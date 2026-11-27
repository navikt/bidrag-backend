import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { DataViewTableV2 } from "~/components/DataViewTableV2";
import tekster from "~/tekster";

export default function RimeligÅInnvilge() {
  const { data } = useNotatFelles();
  const rimeligÅInnvilge = data.utgift?.rimeligÅInnvilge;
  if (rimeligÅInnvilge == null) return null;
  return (
    <>
      <h2>{tekster.titler.rimeligÅInnvilge}</h2>
      <div style={{ marginTop: "8px", width: "350px" }}>
        <DataViewTableV2
          className={"two_column_view_v2"}
          labelColWidth={"250px"}
          data={[
            {
              label: "Det er rimelig å innvilge særbidrag",
              value: rimeligÅInnvilge.rimeligÅInnvilge ? "Ja" : "Nei",
            },
          ]}
        />
      </div>
      <NotatBegrunnelse data={{ innhold: rimeligÅInnvilge.begrunnelse }} />
    </>
  );
}
