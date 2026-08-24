import { exec } from "child_process";
import os from "os";
import { TokenService } from "./TokenService";

export class TerminalService {
  static async copyToClipboard(value: string) {
    (await import("clipboardy")).default.writeSync(value);
    console.log("Copied to clipboard");
  }

  static async formaterBisys() {
    exec(`mvn com.spotify.fmt:fmt-maven-plugin:format`).stdout?.pipe(
      process.stdout,
    );
  }

  static async formaterKtlint() {
    exec(`mvn antrun:run@ktlint`).stdout?.pipe(process.stdout);
  }

  static async creatAivenService() {
    console.log("Rydder opp eksisterende aiven secrets og oppretter ny");
    await runExecSync(`nais aiven tidy`);
    console.log("Oppretter ny aiven service");
    const cluster = await TokenService.getNaisCluster();
    return new Promise<string>((resolve, reject) => {
      exec(
        `nais aiven create -p ${cluster.startsWith("prod") ? "nav-prod" : "nav-dev"} kafka  bidrag-cli bidrag`,
        (error, stdout, stderr) => {
          if (error) {
            console.error(`Error: ${error.message} ${stderr}`, error);
            return;
          }
          if (stderr) {
            console.error(`Stderr: ${stderr}`);
            return;
          }
          const match = stdout.match(/nais aiven get kafka (.+)/);
          const secretName = match[1].trim();
          console.log(`Aiven secret navn: ${secretName}`);
          resolve(this.fetchAivenSecretsPath(secretName));
        },
      );
    });
  }

  static async fetchAivenSecretsPath(secretName: string) {
    return new Promise<string>((resolve, reject) => {
      exec(`nais aiven get kafka ${secretName}`, (error, stdout, stderr) => {
        if (error) {
          console.error(`Error: ${error.message}`);
          return;
        }
        if (stderr) {
          console.error(`Stderr: ${stderr}`);
          return;
        }
        const match = stdout.match(/\/[^\s]+/);
        console.log(`Fant Aiven secret lokasjon: ${match[0]}`);
        resolve(match[0]);
      });
    });
  }
}

function runExecSync(command: string): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    exec(command, (error, stdout, stderr) => {
      if (error) {
        console.error(`Error: ${error.message}`);
        reject(error);
        return;
      }
      if (stderr) {
        console.error(`Stderr: ${stderr}`);
      }
      console.log(stdout);
      resolve(stdout);
    });
  });
}
