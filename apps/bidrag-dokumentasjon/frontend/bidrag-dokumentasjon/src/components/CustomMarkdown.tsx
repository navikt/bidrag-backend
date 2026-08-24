import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { dark } from "react-syntax-highlighter/dist/esm/styles/prism";
const CustomTable = ({ children }) => (
    <div className="overflow-x-auto pb-4 ">
      <table className="markdown-table border-collapse border border-gray-300 w-full">{children}</table>
    </div>
);
const CustomTableRow = ({ children }) => <tr className="bg-gray-100 border-b border-gray-300">{children}</tr>;

const CustomTableCell = ({ children }) => <td className="p-1 border border-gray-300 align-top">{children}</td>;
const CustomTableHeader = ({ children }) => (
    <th className="p-1 border border-gray-300 bg-gray-200 font-semibold align-top">{children}</th>
);
const CustomBlockQuote = ({ children }) => (
    <blockquote className="bg-gray-100 border-l-4 border-gray-600 px-6 py-2 my-4 text-lg italic">{children}</blockquote>
);
const CustomParagraph = ({ children }) => <p className="text-gray-800 leading-relaxed mb-4">{children}</p>;

const CustomEmphasis = ({ children }) => <em className="text-blue-500">{children}</em>;

const CustomStrong = ({ children }) => <strong className="text-black not-italic">{children}</strong>;

const CustomLink = ({ href, children }) => (
    <a href={href} className="text-green-500 underline" target="_blank" rel="noopener noreferrer">
      {children}
    </a>
);
const CustomH2 = ({ children }) => <h2 className="text-2xl font-bold">{children}</h2>;

const CustomH3 = ({ children }) => <h3 className="text-xl font-bold">{children}</h3>;

const CustomH4 = ({ children }) => <h4 className="text-lg font-bold">{children}</h4>;

const CustomH5 = ({ children }) => <h5 className="text-base font-bold">{children}</h5>;

const CustomH6 = ({ children }) => <h6 className="text-sm font-bold">{children}</h6>;
const CustomH1 = ({ children }) => <h1 className="text-3xl font-bold mb-4">{children}</h1>;
const CustomUl = ({ children }) => <ul className="list-disc pl-8">{children}</ul>;

export const MarkdownComponents = {
  code(props) {
    const { children, className, node, ...rest } = props;
    const match = /language-(\w+)/.exec(className || "");
    return match ? (
        <SyntaxHighlighter
            {...rest}
            PreTag="div"
            children={String(children).replace(/\n$/, "")}
            language={match[1]}
            style={dark}
        />
    ) : (
        <code {...rest} className={className}>
          {children}
        </code>
    );
  },
  table: CustomTable,
  tr: CustomTableRow,
  td: CustomTableCell,
  th: CustomTableHeader,
  blockquote: CustomBlockQuote,
  p: CustomParagraph,
  em: CustomEmphasis,
  strong: CustomStrong,
  a: CustomLink,
  h2: CustomH2,
  h3: CustomH3,
  h4: CustomH4,
  h5: CustomH5,
  h6: CustomH6,
  h1: CustomH1,
  ul: CustomUl,
};
