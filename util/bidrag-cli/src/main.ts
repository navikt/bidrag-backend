import { Command } from "commander";
import { default as initKafka } from "./programs/kafka";
import { default as initToken } from "./programs/token";
import { default as initRequest } from "./programs/request";
import { default as initGlobalConfig } from "./programs/globalConfig";
import init from "./env";
const program = new Command();

init();

program
  .name("bidrag-cli")
  .description("CLI verktøy for bidrag")
  .summary(
    "Kan brukes for å opprette og hente testdata fra applikasjoner. I tillegg kan dette brukes for å hente Azure token for ulike applikasjoner",
  )
  .version("0.0.1");

initKafka(program);
initToken(program);
initRequest(program);
initGlobalConfig(program);

program.parse();
