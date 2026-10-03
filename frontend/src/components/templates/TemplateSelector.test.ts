import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'
import TemplateSelector from './TemplateSelector.vue'
import type { CvTemplate } from '../../shared/api/cvTypes'

const templates: CvTemplate[] = [
  { id: 'modern-id', name: 'Modern', description: null, templateKey: 'modern', version: 1, active: true, createdAt: '' },
  { id: 'classic-id', name: 'Classic', description: null, templateKey: 'classic', version: 1, active: true, createdAt: '' },
]

const VBtnToggleStub = defineComponent({
  name: 'VBtnToggle',
  props: { modelValue: { type: String, default: '' }, disabled: Boolean },
  emits: ['update:modelValue'],
  template: '<div><slot /></div>',
})

const VBtnStub = defineComponent({
  name: 'VBtn',
  template: '<button><slot /></button>',
})

describe('TemplateSelector', () => {
  it('renders available templates and forwards the selected template id', async () => {
    const wrapper = mount(TemplateSelector, {
      props: { templates, modelValue: 'modern-id' },
      global: { stubs: { VBtnToggle: VBtnToggleStub, VBtn: VBtnStub } },
    })

    expect(wrapper.get('[role="group"]').attributes('aria-label')).toBe('CV template')
    expect(wrapper.text()).toContain('Modern')
    expect(wrapper.text()).toContain('Classic')

    wrapper.findComponent({ name: 'VBtnToggle' }).vm.$emit('update:modelValue', 'classic-id')

    expect(wrapper.emitted('update:modelValue')).toEqual([['classic-id']])
  })
})