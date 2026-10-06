<template>
  <article class="cv-document" :class="`cv-document--${variant}`">
    <header class="document-header">
      <div class="document-title">
        <p class="document-eyebrow">{{ cv.name }}</p>
        <h2>{{ person.firstName }} {{ person.lastName }}</h2>
        <p v-if="person.position && variant !== 'minimal'" class="document-position">{{ person.position }}</p>
      </div>
      <div v-if="variant !== 'minimal'" class="document-contacts">
        <span v-for="contact in sortedContacts" :key="contact.id" :title="contact.type">{{ contact.value }}</span>
      </div>
    </header>

    <div class="document-body">
      <aside v-if="variant === 'minimal'" class="document-rail">
        <p v-if="person.position" class="document-position">{{ person.position }}</p>
        <h3>Contact</h3>
        <span v-for="contact in sortedContacts" :key="contact.id" :title="contact.type">{{ contact.value }}</span>
      </aside>
      <CvPreviewSections :content="content" :variant="variant" />
    </div>

    <footer class="document-footer">{{ cv.language.toUpperCase() }}</footer>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Cv, CvContent } from '../../shared/api/cvTypes'
import type { Person } from '../../shared/api/personTypes'
import CvPreviewSections from './CvPreviewSections.vue'

const props = defineProps<{
  cv: Cv
  person: Person
  content: CvContent
  variant: 'modern' | 'classic' | 'minimal'
}>()

const sortedContacts = computed(() => [...props.person.contacts].sort((left, right) => left.sortOrder - right.sortOrder))
</script>

<style scoped>
.cv-document { width: 100%; min-height: 620px; padding: clamp(22px, 4vw, 42px); background: #fff; color: #27362e; box-shadow: 0 2px 12px rgb(27 48 36 / 10%); overflow-wrap: anywhere; }
.document-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; padding-bottom: 18px; border-bottom: 3px solid #347463; }
.document-title { min-width: 0; }
.document-eyebrow { margin: 0 0 7px; color: #477461; font-size: 9px; font-weight: 700; text-transform: uppercase; }
.document-title h2 { margin: 0; color: #20382e; font: 700 clamp(22px, 3vw, 31px)/1.1 'Manrope', sans-serif; overflow-wrap: anywhere; }
.document-position { margin: 6px 0 0; color: #647369; font-size: 11px; }
.document-contacts { display: grid; max-width: 42%; gap: 4px; justify-items: end; color: #53645a; font-size: 9px; text-align: right; }
.document-contacts span, .document-rail span { overflow-wrap: anywhere; }
.document-body { padding-top: 21px; }
.document-footer { margin-top: 20px; color: #88948c; font-size: 8px; text-align: right; text-transform: uppercase; }
.cv-document--classic { color: #282828; font-family: Georgia, 'Times New Roman', serif; }
.cv-document--classic .document-header { align-items: center; flex-direction: column; border-bottom: 1px solid #696969; text-align: center; }
.cv-document--classic .document-eyebrow { color: #646464; }
.cv-document--classic .document-title h2 { color: #222; font: 700 28px/1.2 Georgia, 'Times New Roman', serif; }
.cv-document--classic .document-position { color: #555; font-family: Georgia, 'Times New Roman', serif; }
.cv-document--classic .document-contacts { display: flex; max-width: none; flex-wrap: wrap; justify-content: center; margin-top: 10px; text-align: center; }
.cv-document--minimal { padding: clamp(18px, 3vw, 30px); border-top: 5px solid #596f62; }
.cv-document--minimal .document-header { padding-bottom: 13px; border: 0; }
.cv-document--minimal .document-eyebrow { color: #6a7b70; }
.cv-document--minimal .document-title h2 { color: #28352d; font-size: 25px; }
.cv-document--minimal .document-body { display: grid; grid-template-columns: minmax(100px, 0.28fr) minmax(0, 1fr); gap: 20px; padding-top: 8px; }
.document-rail { display: grid; align-content: start; gap: 7px; min-width: 0; padding-right: 13px; border-right: 1px solid #dce4de; color: #56675c; font-size: 9px; }
.document-rail .document-position { margin: 0 0 7px; }
.document-rail h3 { margin: 0; color: #334d3f; font-size: 9px; text-transform: uppercase; }
.cv-document--minimal .document-footer { margin-top: 14px; }
@media (max-width: 560px) {
  .cv-document { min-height: 480px; padding: 20px 16px; }
  .document-header { align-items: flex-start; flex-direction: column; gap: 9px; }
  .document-contacts { max-width: 100%; justify-items: start; text-align: left; }
  .cv-document--classic .document-contacts { justify-content: center; }
  .cv-document--minimal .document-body { grid-template-columns: minmax(0, 1fr); gap: 14px; }
  .document-rail { grid-template-columns: repeat(2, minmax(0, 1fr)); padding: 0 0 12px; border-right: 0; border-bottom: 1px solid #dce4de; }
  .document-rail .document-position { grid-column: 1 / -1; }
  .document-rail h3 { grid-column: 1 / -1; }
}
</style>