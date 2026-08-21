import { DateToDDNameYYYYString, dateOrNull } from "~/utils/date-utils";
import {
  useNotatFelles,
  RenderMode,
} from "~/components/notat_felles/NotatContext";

export default function DagensDato() {
  const { renderMode, data } = useNotatFelles();
  if (renderMode == RenderMode.HTML) return null;
  return (
    <p style={{ position: "absolute", float: "left", right: 0 }}>
      {DateToDDNameYYYYString(
        dateOrNull(data?.vedtak?.fattetTidspunkt) ?? new Date(),
      )}
    </p>
  );
}
