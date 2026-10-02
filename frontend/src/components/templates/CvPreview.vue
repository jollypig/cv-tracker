<template>
  <section class="preview-panel" aria-labelledby="preview-title">
    <div class="preview-toolbar">
      <!-- <div class="preview-toolbar-heading">
        <h2 id="preview-title">Live preview</h2>
        <span v-if="activeTemplate">{{ activeTemplate.name }}</span>
      </div> -->
      <TemplateSelector
        v-if="templates.length"
        :templates="templates"
        :model-value="displayedTemplateId"
        :disabled="disabled"
        @update:model-value="emit('select-template', $event)"
      />
    </div>

    <div v-if="loading" class="preview-state" role="status">Loading preview...</div>
    <div v-else-if="!activeTemplate" class="preview-state">No CV templates are available.</div>
    <div v-else class="preview-stage">
      <component
        :is="templateComponent"
        :key="activeTemplate.id"
        :cv="cv"
        :person="person"
        :content="content"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Cv, CvContent, CvTemplate } from '../../shared/api/cvTypes'
import type { Person } from '../../shared/api/personTypes'
import ClassicTemplate from './ClassicTemplate.vue'
import MinimalTemplate from './MinimalTemplate.vue'
import ModernTemplate from './ModernTemplate.vue'
import TemplateSelector from './TemplateSelector.vue'

const props = defineProps<{
  cv: Cv
  person: Person
  content: CvContent
  templates: CvTemplate[]
  selectedTemplateId: string
  loading?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  'select-template': [templateId: string]
}>()

const activeTemplate = computed(() => props.templates.find((template) => template.id === props.selectedTemplateId)
  ?? props.templates[0])
const displayedTemplateId = computed(() => activeTemplate.value?.id ?? '')
const templateComponent = computed(() => {
  switch (activeTemplate.value?.templateKey) {
    case 'classic': return ClassicTemplate
    case 'minimal': return MinimalTemplate
    default: return ModernTemplate
  }
})
</script>

<style scoped>
.preview-panel { position: sticky; top: 18px; min-width: 0; align-self: start; }
.preview-toolbar { display: grid; gap: 10px; margin-bottom: 12px; }
.preview-toolbar-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; }
.preview-toolbar-heading h2 { margin: 0; color: #293a31; font: 700 16px/1.35 'Manrope', sans-serif; }
.preview-toolbar-heading span { color: #738078; font-size: 11px; }
.preview-stage { max-height: calc(100vh - 180px); min-height: 460px; overflow: auto; padding: 18px; background-color: #e9eeea; background-image: radial-gradient(#cfd9d1 0.7px, transparent 0.7px); background-size: 12px 12px; }
.preview-state { display: grid; min-height: 220px; place-items: center; color: #738078; background: #edf1ed; font-size: 13px; }
@media (max-width: 980px) {
  .preview-panel { position: static; }
  .preview-stage { max-height: 680px; }
}
@media (max-width: 560px) {
  .preview-stage { min-height: 360px; padding: 10px; }
}
</style>