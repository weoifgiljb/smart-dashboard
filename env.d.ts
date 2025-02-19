/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE: string;
  readonly VITE_ENV: "dev" | "prod";
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
