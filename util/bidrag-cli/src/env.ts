import fs from "fs";
import dotenv from "dotenv";

export default function init() {
  return new Promise((resolve) => {
    const pathDocuments = "mockdata/env/documents";
    const pathEnv = "mockdata/env";
    let resolved = 0;
    fs.readdir(pathEnv, (err, files) => {
      files
        ?.filter((f) => f != "documents")
        .forEach((file) => {
          dotenv.config({ path: `${pathEnv}/${file}` });
          resolved += 1;
          if (resolved == 2) resolve("");
        });
    });

    fs.readdir(pathDocuments, (err, files) => {
      files?.forEach((file) => {
        const dotIndex = file.indexOf(".");
        process.env[file.substring(0, dotIndex > -1 ? dotIndex : file.length)] =
          `file:${pathDocuments}/${file}`;
        resolved += 1;
        if (resolved == 2) resolve("");
      });
    });
  });
}
