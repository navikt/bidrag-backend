import fs from "fs";
import path from "path";
export function getAbsolutePath(relativePath: string): string {
  const currentDir = process.cwd();
  return path.resolve(currentDir, relativePath);
}

export async function getFileAsJson(filePath: string): Promise<any> {
  try {
    const fileContent = await fs.promises.readFile(
      getAbsolutePath(filePath),
      "utf-8",
    );
    return JSON.parse(fileContent);
  } catch (error) {
    console.error(`Error reading or parsing file at ${filePath}:`, error);
    throw error;
  }
}
