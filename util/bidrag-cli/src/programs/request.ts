import { Command } from "commander";
import { RequestOption } from "../types";
import FileService from "../services/FileService";
import { RestService } from "../services/RestService";
import { config } from "../config";
const chalk = require("chalk");
require("dotenv").config({ path: "mockdata/env/.documents.env" });

export default function init(program: Command) {
  program
    .command("request")
    .description("Kjør en API kall med config hentet fra fil")
    .argument("<string>", "Plassering til config fil")
    .option("--q1", "Bruk Q1 som miljø")
    .option("--q2", "Bruk Q2 som miljø")
    .action(async (configPath: any, options: any) => {
      config.set("env", options.q2 ? "q2" : "q1");
      const file = await FileService.readFile(configPath);
      const jsonData = JSON.parse(file) as RequestOption;
      const result = await RestService.execute(jsonData);
      console.log("Got respons from request: ", result);
    });
}
