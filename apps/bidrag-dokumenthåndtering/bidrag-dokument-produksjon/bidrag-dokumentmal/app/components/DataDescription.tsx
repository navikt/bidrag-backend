import { ReactElement, CSSProperties } from "react";

type DatadisplayProps = {
  label: string;
  value: ReactElement | string | undefined | number;
  style?: CSSProperties;
};
export default function DataDescription({
  label,
  value,
  style,
}: DatadisplayProps) {
  return (
    <dl className="datarow" style={style}>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </dl>
  );
}
