export default function InntektTableTitle({
  title,
  subtitle,
  witMarginTop,
}: {
  title?: string;
  subtitle?: string;
  witMarginTop?: boolean;
}) {
  if (!title) return null;
  return (
    <div
      className={`inline-block align-middle mb-2 ${witMarginTop ? "mt-4" : ""}`}
    >
      <h3 style={{ padding: 0, margin: "0 0 5px 0", display: "inline" }}>
        {title}
      </h3>
      {subtitle && <span>{subtitle}</span>}
    </div>
  );
}
