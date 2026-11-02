import { generateKeyPairSync, randomUUID } from "node:crypto";
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const envFile = fileURLToPath(new URL("../../.env.texas", import.meta.url));
const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
const jwk = privateKey.export({ format: "jwk" });

// Skriptet skriver `AZURE_APP_JWK` til `.env.texas`, som er ignorert av Git. Fila
// trengs for å kjøre lokal Texas-sidecar og blir ikke overskrevet hvis den finnes.
// Slett den selv og kjør skriptet på nytt hvis du trenger en ny nøkkel.

try {
  writeFileSync(envFile, `AZURE_APP_JWK='${JSON.stringify({ ...jwk, kid: randomUUID(), use: "sig", alg: "RS256" })}'\n`, {
    flag: "wx",
    mode: 0o600,
  });
} catch (error) {
  if (error.code !== "EEXIST") throw error;
  console.warn(".env.texas finnes allerede. Slett fila hvis du vil lage en ny nøkkel.");
}
