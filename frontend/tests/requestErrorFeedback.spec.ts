import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AxiosError } from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

// 不加载真实 router（它会连带拉进 Pinia 与路由守卫）。
// 这里显式指定当前路由为 /login —— 因为拦截器的"401 静默"是否生效，
// 取决于 onPublicAuthPage() 的判断，即当前路径必须是 /login 或 /register。
vi.mock('@/router', () => ({
  default: {
    currentRoute: { value: { path: '/login', fullPath: '/login' } },
    push: vi.fn(),
  },
}))

function failWith(status: number, data: unknown) {
  request.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
    const response = {
      data,
      status,
      statusText: String(status),
      headers: {},
      config,
    } as AxiosResponse
    throw new AxiosError(`HTTP ${status}`, 'ERR_BAD_REQUEST', config, undefined, response)
  }
}

describe('request.ts 错误提示契约', () => {
  let errorSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    errorSpy = vi.spyOn(ElMessage, 'error').mockImplementation((() => {}) as never)
  })

  it('注册失败（409）由拦截器弹出服务端文案', async () => {
    // 这条是判断 Register.vue 里那个"反写守卫"是否算 bug 的判据：
    // 注册失败走 409/400，不落进 401 分支，所以拦截器会提示；
    // 页面再提示一次就重复了 —— 页面侧保持沉默是对的。
    failWith(409, { message: '用户名已存在' })

    await expect(request.post('/auth/register', {})).rejects.toBeTruthy()

    expect(errorSpy).toHaveBeenCalledWith('用户名已存在')
  })

  it('登录失败（401）拦截器保持沉默，必须由页面负责提示', async () => {
    // 这是 Login.vue 那段 catch 存在的理由，也解释了它修之前为什么"毫无反应"。
    failWith(401, { message: '用户名或密码错误' })

    await expect(request.post('/auth/login', {})).rejects.toBeTruthy()

    expect(errorSpy).not.toHaveBeenCalled()
  })

  it('服务端 5xx 由拦截器弹出服务端文案', async () => {
    failWith(500, { message: '服务器内部错误' })

    await expect(request.post('/diaries', {})).rejects.toBeTruthy()

    expect(errorSpy).toHaveBeenCalledWith('服务器内部错误')
  })
})
