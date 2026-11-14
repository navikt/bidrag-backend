import { reactRouter } from "@react-router/dev/vite";
import { defineConfig } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";
import { teksterYaml } from "./scripts/tekster";

export default defineConfig({
  server: {
    port: 3001,
  },
  plugins: [teksterYaml(), reactRouter(), tsconfigPaths()],
});
