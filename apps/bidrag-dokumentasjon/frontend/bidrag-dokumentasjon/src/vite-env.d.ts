/// <reference types="vite/client" />

interface Window {
  callbackKotlin?: (link: string) => void;
  visGrunnlag?: (link: string) => void;
  visMarkdown?: (innhold: string) => void;
  visApplikasjon?: (applicationId: string) => void;
  visDiagram?: (diagramPath: string) => void;
}

