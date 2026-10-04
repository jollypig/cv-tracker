<template>
  <div class="template-selector" role="group" :aria-label="translate('template.selector')">
    <v-btn-toggle
      :model-value="modelValue"
      :disabled="disabled"
      color="primary"
      divided
      mandatory
      class="template-options"
      @update:model-value="emit('update:modelValue', $event)"
    >
      <v-btn v-for="template in templates" :key="template.id" :value="template.id" class="template-option">
        <span class="template-sample" :class="`template-sample--${template.templateKey}`" aria-hidden="true">
          <span /><span /><span />
        </span>
        <span class="template-option-name">{{ template.name }}</span>
      </v-btn>
    </v-btn-toggle>
  </div>
</template>

<script setup lang="ts">
import type { CvTemplate } from '../../shared/api/cvTypes'
import { translate } from '../../shared/i18n'

defineProps<{
  templates: CvTemplate[]
  modelValue: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<style scoped>
.template-selector { min-width: 0; }
.template-options { display: flex; width: 100%; }
.template-option { display: flex; width: 0; min-width: 0; height: auto; min-height: 48px; flex: 1 1 0; flex-direction: column; gap: 4px; padding: 6px 4px; border-radius: 4px; }
.template-option-name { overflow: hidden; max-width: 100%; color: #33433a; font-size: 11px; line-height: 1.2; text-overflow: ellipsis; }
.template-option.v-btn--active .template-option-name { color: #fff; }
.template-sample { display: flex; width: 30px; height: 22px; flex-direction: column; justify-content: center; gap: 3px; padding: 4px; border: 1px solid #c8d2ca; border-radius: 2px; background: #fff; }
.template-sample span { display: block; height: 2px; background: #83978a; }
.template-sample span:first-child { width: 70%; height: 3px; background: #347463; }
.template-sample--classic { align-items: center; }
.template-sample--classic span { width: 80%; }
.template-sample--classic span:first-child { width: 55%; background: #51575a; }
.template-sample--minimal { align-items: flex-end; border-left: 4px solid #d8e6df; }
.template-sample--minimal span { width: 66%; }
.template-sample--minimal span:first-child { width: 72%; background: #546b5f; }
</style>