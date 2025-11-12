import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendPort = env.VITE_BACKEND_PORT || '8080'
  // GitHub Pages base 路径配置
  // 如果仓库名是 username.github.io，则 base 为 '/'
  // 如果仓库名是其他名称，则 base 为 '/仓库名/'
  const base = env.VITE_BASE_PATH || '/'

  const bundleBudget = () => {
    // 调整大小限制：JS 文件 1.5MB，CSS 文件 500KB
    // Element Plus 和 ECharts 等大型库本身较大，这是正常的
    const MAX_JS_BYTES = 1.5 * 1024 * 1024 // 1.5MB for JS
    const MAX_CSS_BYTES = 500 * 1024 // 500KB for CSS
    return {
      name: 'bundle-size-budget',
      generateBundle(_: unknown, bundle: Record<string, any>) {
        const overs: string[] = []
        for (const [fileName, chunk] of Object.entries(bundle)) {
          const size = (chunk as any).code ? Buffer.byteLength((chunk as any).code) : (chunk as any).source?.length
          if (typeof size === 'number') {
            const maxSize = fileName.endsWith('.js') ? MAX_JS_BYTES : MAX_CSS_BYTES
            if (size > maxSize && /\.js$|\.css$/.test(fileName)) {
              overs.push(`${fileName} ${(size / 1024).toFixed(1)}KB`)
            }
          }
        }
        if (overs.length) {
          this.error(`Bundle size over budget:\n${overs.map((s) => ' - ' + s).join('\n')}`)
        }
      }
    }
  }

  return {
    base,
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





