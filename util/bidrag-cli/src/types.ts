export interface RequestOption {
  url: string | UrlEnvConfig;
  token?: string | AzureConfig;
  method: "post" | "get" | "patch";
  headers?: Record<string, string>;
  body?: any;
}

export interface UrlEnvConfig {
  q1: string;
  q2: string;
}

export interface AzureConfig {
  [env: string]: {
    fromApp: string;
    scope: string;
  };
}
