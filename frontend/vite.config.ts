import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendPort = env.VITE_BACKEND_PORT || '8080'

  const bundleBudget = () => {
    const MAX_ASSET_BYTES = 300 * 1024 // 300KB 单文件限制（可按需调整）
    return {
      name: 'bundle-size-budget',
      generateBundle(_: unknown, bundle: Record<string, any>) {
        const overs: string[] = []
        for (const [fileName, chunk] of Object.entries(bundle)) {
          const size = (chunk as any).code ? Buffer.byteLength((chunk as any).code) : (chunk as any).source?.length
          if (typeof size === 'number' && size > MAX_ASSET_BYTES && /\.js$|\.css$/.test(fileName)) {
            overs.push(`${fileName} ${(size / 1024).toFixed(1)}KB`)
          }
        }
        if (overs.length) {
          this.error(`Bundle size over budget:\n${overs.map((s) => ' - ' + s).join('\n')}`)
        }
      }
    }
  }

  return {
    plugins: [vue(), bundleBudget()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: `http://localhost:${backendPort}`,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          ws: true,
          secure: false
        }
      }
    },
    esbuild: {
      drop: mode === 'production' ? ['console', 'debugger'] : []
    },
    build: {
      sourcemap: false,
      chunkSizeWarningLimit: 1200,
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'pinia'],
            'vendor-element-plus': ['element-plus', '@element-plus/icons-vue'],
            'vendor-axios': ['axios'],
            'vendor-echarts': ['echarts']
          }
        }
      }
    }
  }
})





