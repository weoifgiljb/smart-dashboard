/// <reference types="vite/client" />
/// <reference types="vite-plugin-react/swc" />

interface ImportMetaEnv {
  readonly VITE_OPENWEATHER_API_KEY: string;
  readonly VITE_DEFAULT_LAT: string;
  readonly VITE_DEFAULT_LON: string;
  readonly VITE_APP_TITLE: string;
  readonly VITE_API_BASE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
