/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_OPENWEATHER_API_KEY: string;
  readonly VITE_DEFAULT_LAT: string;
  readonly VITE_DEFAULT_LON: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
