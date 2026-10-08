import { h } from 'vue'
import type { SetupContext } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { MockInstance } from 'vitest'
import ElementPlus, { ElMessage } from 'element-plus'
import Login from './Login.vue'

const { push, loginUser } = vi.hoisted(() => ({
  push: vi.fn(),
  loginUser: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ query: {} }),
}))

vi.mock('@/store/user', () => ({
  useUserStore: () => ({ loginUser }),
}))

// el-form 的 validate 直接回调 true，把断言集中在"登录失败有没有反馈"上。
const ElFormStub = {
  name: 'ElForm',
  setup(_props: unknown, ctx: SetupContext) {
    ctx.expose({ validate: (cb: (valid: boolean) => void) => cb(true) })
    return () => h('form', ctx.slots.default ? ctx.slots.default() : [])
  },
}

function axiosError(status: number, data?: unknown) {
  return { isAxiosError: true, response: { status, data } }
}

async function submitLogin() {
  const wrapper = mount(Login, {
    global: {
      plugins: [ElementPlus],
      stubs: { 'el-form': ElFormStub },
    },
  })
  const submit = wrapper.findAll('button').find((b) => b.text().includes('立即登录'))
  if (!submit) throw new Error('未找到登录按钮')
  await submit.trigger('click')
  await flushPromises()
  return wrapper
}

describe('Login.vue 登录失败的反馈（Issue #3 Bug 1 & 2）', () => {
  let errorSpy: MockInstance<typeof ElMessage.error>

  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessage, 'success').mockImplementation((() => {}) as never)
    errorSpy = vi.spyOn(ElMessage, 'error').mockImplementation((() => {}) as never)
  })

  it('401 时展示服务端返回的文案', async () => {
    loginUser.mockRejectedValue(axiosError(401, { message: '用户名或密码错误' }))
    await submitLogin()
    expect(errorSpy).toHaveBeenCalledWith('用户名或密码错误')
  })

  it('401 且服务端未给文案时使用兜底文案', async () => {
    loginUser.mockRejectedValue(axiosError(401, {}))
    await submitLogin()
    expect(errorSpy).toHaveBeenCalledWith('用户名或密码错误')
  })

  it('非 401 的 axios 错误不重复提示（交给拦截器）', async () => {
    loginUser.mockRejectedValue(axiosError(500, { message: '服务器繁忙' }))
    await submitLogin()
    expect(errorSpy).not.toHaveBeenCalled()
  })

  it('非 axios 错误给出兜底提示', async () => {
    loginUser.mockRejectedValue(new Error('boom'))
    await submitLogin()
    expect(errorSpy).toHaveBeenCalledWith('登录失败，请重试')
  })

  it('登录成功后跳转', async () => {
    loginUser.mockResolvedValue(undefined)
    await submitLogin()
    expect(push).toHaveBeenCalled()
  })
})
