import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'
import TemplateSelector from './TemplateSelector.vue'
import CvPreview from './CvPreview.vue'
import CvTemplateDocument from './CvTemplateDocument.vue'
import type { Cv, CvContent, CvTemplate } from '../../shared/api/cvTypes'
import type { Person } from '../../shared/api/personTypes'

const templates: CvTemplate[] = [
  { id: 'modern-id', name: 'Modern', description: null, templateKey: 'modern', version: 1, active: true, createdAt: '' },
  { id: 'classic-id', name: 'Classic', description: null, templateKey: 'classic', version: 1, active: true, createdAt: '' },
  { id: 'minimal-id', name: 'Minimal', description: null, templateKey: 'minimal', version: 1, active: true, createdAt: '' },
]

const cv: Cv = { id: 'cv', personId: 'person', personName: 'Jane Doe', name: 'Backend CV', description: null,
  language: 'en', status: 'DRAFT', templateId: null, currentVersionId: null, tags: [], createdAt: '', updatedAt: '' }
const person: Person = { id: 'person', firstName: 'Jane', lastName: 'Doe', position: 'Engineer', location: 'Riga',
  dateOfBirth: null, gender: null, maritalStatus: null, militaryStatus: null, photoStorageKey: null,
  contacts: [{ id: 'hidden', type: 'EMAIL', value: 'hidden@example.com', primary: false, sortOrder: 0, showContact: false },
    { id: 'visible', type: 'PHONE', value: '+371 20000000', primary: false, sortOrder: 1, showContact: true }],
  createdAt: '', updatedAt: '' }
const content: CvContent = { summary: 'Profile summary', includeSkillDetailsInOutput: false, includeSkillLevelsInOutput: true,
  experiences: [], education: [], skillGroups: [], languages: [], projects: [], certifications: [], customSections: [], sections: [] }

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
  it.each(['classic', 'minimal', 'modern'] as const)('previews location and visible contacts for %s', (variant) => {
    const wrapper = mount(CvTemplateDocument, { props: { cv, person, content, variant } })
    expect(wrapper.classes()).toContain(`cv-document--${variant}`)
    expect(wrapper.text()).toContain('Riga')
    expect(wrapper.text()).toContain('Engineer')
    expect(wrapper.text()).toContain('+371 20000000')
    expect(wrapper.text()).toContain('Profile summary')
    expect(wrapper.text()).toContain('Jane Doe')
    expect(wrapper.text()).not.toContain(cv.name)
    expect(wrapper.text()).not.toContain('hidden@example.com')
    if (variant === 'minimal') expect(wrapper.get('.document-rail').text()).toContain('Riga')
  })

  it.each(['classic', 'minimal', 'modern'] as const)('does not use the CV name when the person name is blank for %s', (variant) => {
    const wrapper = mount(CvTemplateDocument, {
      props: { cv, person: { ...person, firstName: '', lastName: '' }, content, variant },
    })
    expect(wrapper.text()).not.toContain(cv.name)
    expect(wrapper.find('.document-title h2').exists()).toBe(false)
  })

  it.each(templates)('uses the selected $name template rather than list order', (selected) => {
    const wrapper = mount(CvPreview, { props: { cv, person, content, templates, selectedTemplateId: selected.id },
      global: { stubs: { TemplateSelector: true } } })
    expect(wrapper.get('.cv-document').classes()).toContain(`cv-document--${selected.templateKey}`)
  })

  it('defaults to Modern even when Classic is listed first', () => {
    const wrapper = mount(CvPreview, { props: { cv, person, content, templates: [...templates].reverse(), selectedTemplateId: '' },
      global: { stubs: { TemplateSelector: true } } })
    expect(wrapper.get('.cv-document').classes()).toContain('cv-document--modern')
  })

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