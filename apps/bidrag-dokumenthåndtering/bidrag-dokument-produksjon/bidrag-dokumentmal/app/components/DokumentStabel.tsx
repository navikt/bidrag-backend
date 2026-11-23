import {
  Children,
  CSSProperties,
  Fragment,
  isValidElement,
  PropsWithChildren,
  ReactNode,
} from "react";

type DokumentStabelProps = PropsWithChildren<{
  gap: 1 | 2 | 3 | 4;
  className?: string;
}>;

const AVSTAND = {
  1: "4px",
  2: "8px",
  3: "12px",
  4: "16px",
} as const;

function stabelDeler(children: ReactNode): ReactNode[] {
  return Children.toArray(children).flatMap((child) =>
    isValidElement<PropsWithChildren>(child) && child.type === Fragment
      ? stabelDeler(child.props.children)
      : [child],
  );
}

export default function DokumentStabel({
  gap,
  className = "",
  children,
}: DokumentStabelProps) {
  const style: CSSProperties & { "--dokument-stabel-gap": string } = {
    "--dokument-stabel-gap": AVSTAND[gap],
    gap: AVSTAND[gap],
  };
  return (
    <div
      className={`dokument-stabel flex flex-col ${className}`.trim()}
      style={style}
    >
      {stabelDeler(children).map((child, index) => (
        <div className="dokument-stabel-del" key={index}>
          {child}
        </div>
      ))}
    </div>
  );
}
