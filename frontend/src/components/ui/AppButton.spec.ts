import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppButton from './AppButton.vue'
import ElementPlus from 'element-plus'

describe('AppButton.vue', () => {
  const globalConfig = {
    plugins: [ElementPlus],
  }

  it('renders default button correctly', () => {
    const wrapper = mount(AppButton, {
      global: globalConfig,
    })
    expect(wrapper.find('.el-button').exists()).toBe(true)
    expect(wrapper.classes()).toContain('el-button')
  })

  it('renders slot content', () => {
    const wrapper = mount(AppButton, {
      global: globalConfig,
      slots: {
        default: 'Click Me',
      },
    })
    expect(wrapper.text()).toBe('Click Me')
  })

  it('emits click event', async () => {
    const wrapper = mount(AppButton, {
      global: globalConfig,
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('applies variant prop correctly', () => {
    const wrapper = mount(AppButton, {
      global: globalConfig,
      props: {
        variant: 'danger',
      },
    })
    // Element Plus button uses class for styling
    expect(wrapper.classes()).toContain('el-button--danger')
  })

  it('shows loading state', () => {
    const wrapper = mount(AppButton, {
      global: globalConfig,
      props: {
        loading: true,
      },
    })
    expect(wrapper.classes()).toContain('is-loading')
  })
})
