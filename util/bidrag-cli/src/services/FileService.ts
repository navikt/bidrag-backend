import fs from "fs";
import { RequestOption } from "../types.js";

export default class FileService {
  static async readFile(filePath: string): Promise<string> {
    return new Promise((resolve, reject) => {
      fs.readFile(filePath, "utf8", (err: any, data: string) => {
        if (err) {
          console.error(err);
          reject(err);
          return;
        }
        resolve(data);
      });
    });
  }
}
