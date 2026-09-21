import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { nextTick, ref } from 'vue'
import AppPagination from './AppPagination.vue'

const globalConfig = {
  plugins: [ElementPlus],
}

describe('AppPagination.vue', () => {
  it('hides when everything fits on one page', () => {
    const wrapper = mount(AppPagination, {
      global: globalConfig,
      props: { total: 5, pageSize: 5, modelValue: 1 },
    })
    expect(wrapper.find('.app-pagination').exists()).toBe(false)
  })

  it('renders pager when there is more than one page', () => {
    const wrapper = mount(AppPagination, {
      global: globalConfig,
      props: { total: 7, pageSize: 5, modelValue: 1 },
    })
    expect(wrapper.find('.el-pagination').exists()).toBe(true)
    expect(wrapper.text()).toContain('7')
  })

  it('updates v-model and emits change when going to next page', async () => {
    const page = ref(1)
    const wrapper = mount(AppPagination, {
      global: globalConfig,
      props: {
        total: 7,
        pageSize: 5,
        modelValue: page.value,
        'onUpdate:modelValue': (value: number) => {
          page.value = value
        },
      },
    })
    await wrapper.find('.btn-next').trigger('click')
    await nextTick()
    expect(page.value).toBe(2)
    expect(wrapper.emitted('change')?.[0]).toEqual([2])
  })
})
