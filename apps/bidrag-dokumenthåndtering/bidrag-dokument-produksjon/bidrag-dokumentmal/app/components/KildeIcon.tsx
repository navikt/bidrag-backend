import { Kilde } from "~/types/Api";

export default function KildeIcon({ kilde }: { kilde: Kilde }) {
  return (
    <>
      {kilde == Kilde.OFFENTLIG ? <span>Offentlig</span> : <span>Manuell</span>}
    </>
  );
}
