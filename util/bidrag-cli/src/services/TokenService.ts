import { exec } from "child_process";
import { ExecException } from "child_process";
import { config } from "../config";
import chalk from "chalk";
import got from "got";
import { FormData } from "formdata-node"; // or:

const azureProdId = "62366534-1ec3-4962-8869-9b5535279d0b";
const azureDevId = "966ac572-f5b7-4bbe-aa88-c76419c0f851";

export class TokenService {
  static getNaisCluster(): Promise<string> {
    return new Promise((resolve) => {
      exec(
        `kubectl config current-context`,
        (error: ExecException | null, stdout: string, stderr: string) => {
          const cluster = stdout
            .replace("\n", "")
            .replace("nais-dev", "dev-gcp")
            .replace("nais-prod", "prod-gcp");
          config.set("nais_cluster", cluster);
          resolve(cluster);
        },
      );
    });
  }

  static getTokenCredentials(
    appName: string,
  ): Promise<{ id: string; secret: string }> {
    return new Promise((resolve) => {
      exec(
        `kubectl exec --tty deployment/${appName} -n bidrag -- printenv | grep -e AZURE_APP_CLIENT_ID -e AZURE_APP_CLIENT_SECRET`,
        (error: ExecException | null, stdout: string, stderr: string) => {
          const stdoutSplit = stdout.split(/\n/);
          let CLIENT_ID =
            stdoutSplit.find((a) => a.includes("AZURE_APP_CLIENT_ID")) ?? "";
          CLIENT_ID = CLIENT_ID.substring(CLIENT_ID.indexOf("=") + 1);
          let CLIENT_SECRET =
            stdoutSplit.find((a) => a.includes("AZURE_APP_CLIENT_SECRET")) ??
            "";
          CLIENT_SECRET = CLIENT_SECRET.substring(
            CLIENT_SECRET.indexOf("=") + 1,
          );

          process.env.CLIENT_ID = CLIENT_ID;
          process.env.CLIENT_SECRET = CLIENT_SECRET;
          config.set("client_id", CLIENT_ID);
          config.set("client_secret", CLIENT_SECRET);

          resolve({ id: CLIENT_ID, secret: CLIENT_SECRET });
        },
      );
    });
  }

  static getAzureCredentials(appName: string): Promise<any> {
    return new Promise((resolve) => {
      exec(
        `kubectl exec --tty deployment/${appName} -n bidrag -- printenv | grep AZURE_`,
        (error: ExecException | null, stdout: string, stderr: string) => {
          resolve("");
        },
      );
    });
  }

  static fetchOnBehalfOf(assertion: string, scope: string) {
    return new Promise(async (resolve) => {
      const form = new FormData();
      form.set("client_id", config.get("client_id"));
      form.set("scope", scope);
      form.set("client_secret", config.get("client_secret"));
      form.set("assertion", assertion);
      form.set("requested_token_use", "on_behalf_of");
      form.set("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
      form.set(
        "client_assertion_type",
        "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
      );
      const data = await got
        .post(
          `https://login.microsoftonline.com/${"naisCluster"?.startsWith("prod") ? azureProdId : azureDevId}/oauth2/v2.0/token`,
          {
            headers: {
              "Content-Type": "application/x-www-form-urlencoded",
            },
            body: form,
          },
        )
        .json();
    });
  }

  static fetchClientCredentials(
    appName: string,
    _scope?: string,
  ): Promise<string> {
    return new Promise(async (resolve) => {
      const naisCluster: string = config.get("nais_cluster") as string;
      const scope = _scope ?? `${naisCluster}.bidrag.${appName}`;
      console.log(
        chalk.blue(
          `Genererer token for ${chalk.underline(appName)} for scope ${chalk.underline(scope)}`,
        ),
      );
      try {
        const response = await got
          .get(
            `https://login.microsoftonline.com/${naisCluster?.startsWith("prod") ? azureProdId : azureDevId}/oauth2/v2.0/token`,
            {
              get allowGetBody(): boolean {
                return true;
              },
              form: {
                grant_type: "client_credentials",
                client_id: config.get("client_id"),
                scope: `api://${scope}/.default`,
                client_secret: config.get("client_secret"),
              },
            },
          )
          .json();
        // @ts-ignore
        resolve(response.access_token);
      } catch (e) {
        console.log("Det skjedde en feil ved henting av token", e);
      }
    });
  }
}
