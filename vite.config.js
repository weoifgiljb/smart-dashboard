import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
export default defineConfig({
    plugins: [react()],
    test: {
        globals: true,
        environment: "jsdom",
        setupFiles: "./src/setupTests.ts",
        coverage: {
            reporter: ["text", "json", "html"],
        },
    },
    resolve: {
        alias: {
            "@": "/src",
            "@components": "/src/components",
        },
    },
});
//# sourceMappingURL=vite.config.js.map