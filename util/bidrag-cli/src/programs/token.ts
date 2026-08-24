import { Command } from "commander";
import { TokenService } from "../services/TokenService";
import { TerminalService } from "../services/TerminalService";
import chalk from "chalk";

export default function init(program: Command) {
  program
    .command("token")
    .description("Hent Azure token som skal brukes for å kalle en applikasjon")
    .argument("<string>", "Applikasjons navn")
    .option(
      "--scope <string>",
      'Scope som skal brukes når tokenet hentes (eks. "dev-fss.bidrag.bidrag-dokument")',
    )
    .action(async (appName: any, options: any) => {
      await TokenService.getTokenCredentials(appName);
      await TokenService.getNaisCluster();
      const accessToken = await TokenService.fetchClientCredentials(
        appName,
        options.scope,
      );
      await TerminalService.copyToClipboard(accessToken);
      console.log(chalk.whiteBright(accessToken));
    });

  program
    .command("creds")
    .description("Hent Azure creds for applikasjon")
    .argument("<string>", "Applikasjons navn")
    .option("-a", "Hent alle Azure miljøvariabler")
    .action(async (appNavn: any, options: any) => {
      if (options.a) {
        await TokenService.getAzureCredentials(appNavn);
        return;
      }
      const tokenCreds = await TokenService.getTokenCredentials(appNavn);
      console.log(
        chalk.whiteBright(`Azure credentials for app ${chalk.italic(appNavn)}`),
      );
      console.log(
        chalk.red("AZURE_APP_CLIENT_ID=") + chalk.whiteBright(tokenCreds.id),
      );
      console.log(
        chalk.red("AZURE_APP_CLIENT_SECRET=") +
          chalk.whiteBright(tokenCreds.secret),
      );
    });

  program
    .command("formater-bisys")
    .description(
      "Formaterer bisyskode med hjelp av mvn com.spotify.fmt:fmt-maven-plugin:format",
    )
    .action(async () => {
      await TerminalService.formaterBisys();
    });

  program
    .command("formater-ktlint")
    .description("Formaterer kotlinkode med hjelp av antrun:run@ktlint")
    .action(async () => {
      await TerminalService.formaterKtlint();
    });
}
