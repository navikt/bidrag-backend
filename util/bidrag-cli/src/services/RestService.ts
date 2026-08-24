import { RequestOption } from "../types";
import { TokenService } from "./TokenService";
import { config } from "../config";
import fs from "fs";

export class RestService {
  static async execute(requestConfig: RequestOption) {
    const token = await this.getToken(requestConfig);
    return new Promise((resolve) => {
      const options = {
        method: requestConfig.method,
        url: this.getUrl(requestConfig),
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: this.replaceBodyWithEnv(JSON.stringify(requestConfig.body)),
      };
      // request(options, function (error, response) {
      //   if (error) throw new Error(error);
      //   const jsonBody = JSON.parse(response.body)
      //   resolve(jsonBody)
      // });
    });
  }

  private static replaceBodyWithEnv(requestBody: string): string {
    if (requestBody.indexOf("{{") != -1) {
      const envNameWithTags = requestBody.substring(
        requestBody.indexOf("{{"),
        requestBody.indexOf("}}") + 2,
      );
      const envName = envNameWithTags.replace(/{/g, "").replace(/}/g, "");
      let envValue = process.env[envName]!;
      if (envValue && envValue.startsWith("file")) {
        envValue = fs.readFileSync(envValue.replace("file:", ""), {
          encoding: "base64",
        });
      }
      return this.replaceBodyWithEnv(
        requestBody.replace(envNameWithTags, envValue),
      );
    }
    return requestBody;
  }
  private static getUrl(requestConfig: RequestOption) {
    if (typeof requestConfig.url == "string") {
      return requestConfig.url;
    }
    const env = config.get("env") as string;
    // @ts-ignore
    return requestConfig.url[env] as string;
  }

  private static async getToken(requestOption: RequestOption) {
    if (!requestOption.token) {
      return "empty";
    }
    if (typeof requestOption.token == "string") {
      return requestOption.token;
    }
    const env = config.get("env") as string;

    const tokenConfig = requestOption.token?.[env];

    await TokenService.getTokenCredentials(tokenConfig?.fromApp);
    return TokenService.fetchClientCredentials(
      tokenConfig?.fromApp,
      tokenConfig?.scope,
    );
  }
}
