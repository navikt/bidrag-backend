export default function Inntektspost({
  label,
  value,
}: {
  label?: string;
  value?: string | number;
}) {
  return (
    <dl>
      <dt style={{ fontWeight: "normal" }}>{label}:</dt>
      <dd>{value}</dd>
    </dl>
  );
}
