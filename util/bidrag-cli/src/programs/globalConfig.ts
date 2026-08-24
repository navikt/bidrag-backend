import { Command } from "commander";

const chalk = require("chalk");
import Store from "electron-store";

const schema = {
  dataPath: {
    type: "string",
    default: "mockdata/data",
  },
  env: {
    type: "string",
    default: "mockdata/env",
  },
  envdocs: {
    type: "string",
    default: "mockdata/documents",
  },
};
//@ts-ignore
const store = new Store({ projectName: "bidrag-cli", schema });

export default function init(program: Command) {
  const configCommand = program
    .command("config")
    .description("Sett eller oppdater global konfigurasjon")
    .option(
      "--data-path <string>",
      "Plassering på mock data mappen",
      "mockdata/data",
    )
    .option(
      "--env-path <string>",
      "Plassering på miljøvariabler mappe",
      "mockdata/env",
    )
    .option(
      "--documents-path <string>",
      "Plassering på mappe for dokumenter som brukes som miljøvariabler",
      "mockdata/env/documents",
    )
    .action(async (options: any) => {
      console.log(store.get("data_path"));
      if (options.dataPath) {
        store.set("data_path", options.dataPath);
      }
      console.log(store.get("data_path"), options.dataPath, options);
    });

  configCommand
    .command("list")
    .description("Vis alle konfigurasjons variabler")
    .action(() => {
      console.log(store.store, store.path);
    });

  configCommand
    .command("path")
    .description("Vis hvor konfigurasjon er lagret")
    .action(() => {
      console.log(store.path);
    });
}
