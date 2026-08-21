import { Links, Meta, Outlet, Scripts, ScrollRestoration } from "react-router";
import fs from "fs";

const styles = fs.readFileSync("app/style/style.css").toString();
const tailwindStyles = fs.readFileSync("app/style/tw_output.css").toString();

export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="nb-NO">
      <head>
        <meta charSet="utf-8" />
        <Meta />
        <Links />
        <style
          dangerouslySetInnerHTML={{
            __html: styles,
          }}
        />
        <style
          dangerouslySetInnerHTML={{
            __html: tailwindStyles,
          }}
        />
      </head>
      <body>
        {children}
        <ScrollRestoration />
        <Scripts />
      </body>
    </html>
  );
}

export default function App() {
  return <Outlet />;
}
