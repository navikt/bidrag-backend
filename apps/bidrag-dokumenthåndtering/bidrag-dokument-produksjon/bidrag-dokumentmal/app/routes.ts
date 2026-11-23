import { type RouteConfig, prefix, route } from "@react-router/dev/routes";
import { flatRoutes } from "@react-router/fs-routes";

// Notatene har tre adresser per notat:
// /notat/<notat> gir hele notatet, /notat/begrunnelse/<notat> og /notat/opplysninger/<notat> gir hver sin del.
// Mappene under routes/notat følger de samme delene. Se README.md.
const notater = ["forskudd", "særbidrag", "bidrag"];

export default [
  ...(await flatRoutes({ ignoredRouteFiles: ["notat/**"] })),
  ...prefix(
    "notat",
    notater.flatMap((notat) => [
      route(notat, `routes/notat/${notat}.tsx`),
      route(
        `begrunnelse/${notat}`,
        `routes/notat/begrunnelse/${notat}/route.tsx`,
      ),
      route(
        `opplysninger/${notat}`,
        `routes/notat/opplysninger/${notat}/route.tsx`,
      ),
    ]),
  ),
] satisfies RouteConfig;
