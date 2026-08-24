import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { DateToDDNameYYYYString, dateOrNull } from "~/utils/date-utils";

export default function NotatTittel({ title }: { title: string }) {
  return (
    <div className={"flex flex-row justify-between w-[--content-width]"}>
      <h1 className="title2">{title}</h1>
      <DagensDato />
    </div>
  );
}
function DagensDato() {
  const { data } = useNotatFelles();
  return (
    <p className={"self-center"}>
      {DateToDDNameYYYYString(
        dateOrNull(data?.vedtak?.fattetTidspunkt) ?? new Date(),
      )}
    </p>
  );
}
