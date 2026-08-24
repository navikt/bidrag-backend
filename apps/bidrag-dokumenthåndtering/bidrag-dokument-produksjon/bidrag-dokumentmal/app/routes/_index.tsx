import type { MetaFunction } from "react-router";

export const meta: MetaFunction = () => {
  return [
    { title: "bidrag-dokumentmal" },
    { name: "description", content: "bidrag-dokumentmal" },
  ];
};
export default function Index() {
  return (
    <div style={{ fontFamily: "system-ui, sans-serif", lineHeight: "1.8" }}>
      <h1>Velkommen til bidrag-dokumentmal</h1>
    </div>
  );
}
