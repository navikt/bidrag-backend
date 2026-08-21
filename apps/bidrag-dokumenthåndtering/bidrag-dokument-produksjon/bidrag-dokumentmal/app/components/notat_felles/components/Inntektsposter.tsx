import Inntektspost from "~/components/Inntekspost";
import { formatPeriode } from "~/utils/date-utils";
import { formatterBeløp } from "~/utils/visningsnavn";
import { NotatInntektDto, TypeArManedsperiode } from "~/types/Api";
import { useTheme } from "~/components/notat_felles/ThemeContext";

type InntektsposterProps = {
  periode?: TypeArManedsperiode;
  data: NotatInntektDto;
  withHorizontalLine?: boolean;
};
export default function Inntektsposter({
  periode,
  data,
  withHorizontalLine = true,
}: InntektsposterProps) {
  const { styling } = useTheme();
  return (
    <div
      style={{
        width: "100%",
      }}
    >
      <Inntektspost
        label={"Periode"}
        value={formatPeriode(periode!.fom, periode!.til)}
      />
      {data.inntektsposter.map((d, i) => (
        <Inntektspost
          key={d.kode + i.toString()}
          label={d.visningsnavn!}
          value={formatterBeløp(d.beløp)}
        />
      ))}
      {withHorizontalLine && styling == "V1" && (
        <div className="horizontal-line"></div>
      )}
    </div>
  );
}
