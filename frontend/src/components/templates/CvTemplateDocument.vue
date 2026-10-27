<template>
  <article class="cv-document" :class="`cv-document--${variant}`">
    <header class="document-header">
      <img v-if="variant === 'modern' && photoUrl" class="document-photo document-photo--modern" :src="photoUrl" :alt="`${person.firstName} ${person.lastName}`.trim()">
      <div class="document-title">
        <h2 v-if="person.firstName || person.lastName">{{ [person.firstName, person.lastName].filter(Boolean).join(' ') }}</h2>
        <p v-if="person.position && variant !== 'minimal'" class="document-position">{{ person.position }}</p>
      </div>
      <img v-if="variant === 'classic' && photoUrl" class="document-photo document-photo--classic" :src="photoUrl" :alt="`${person.firstName} ${person.lastName}`.trim()">
      <div v-if="variant !== 'minimal' && (person.location || sortedContacts.length > 0)" class="document-contacts">
        <span v-if="person.location">{{ person.location }}</span>
        <span v-for="contact in sortedContacts" :key="contact.id" :title="contact.type">{{ contact.value }}</span>
      </div>
    </header>

    <div class="document-body">
      <aside v-if="variant === 'minimal'" class="document-rail">
        <p v-if="person.position" class="document-position">{{ person.position }}</p>
        <img v-if="photoUrl" class="document-photo document-photo--minimal" :src="photoUrl" :alt="`${person.firstName} ${person.lastName}`.trim()">
        <template v-if="person.location || sortedContacts.length > 0">
          <h3>Contact</h3>
          <span v-if="person.location">{{ person.location }}</span>
          <span v-for="contact in sortedContacts" :key="contact.id" :title="contact.type">{{ contact.value }}</span>
        </template>
      </aside>
      <CvPreviewSections :content="content" :variant="variant" />
    </div>

    <footer class="document-footer">{{ cv.language.toUpperCase() }}</footer>
  </article>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { Cv, CvContent } from '../../shared/api/cvTypes'
import personApi from '../../shared/api/personApi'
import type { Person } from '../../shared/api/personTypes'
import CvPreviewSections from './CvPreviewSections.vue'

const props = defineProps<{
  cv: Cv
  person: Person
  content: CvContent
  variant: 'modern' | 'classic' | 'minimal'
}>()

const sortedContacts = computed(() => [...props.person.contacts]
  .filter((contact) => contact.showContact !== false && contact.value.trim())
  .sort((left, right) => left.sortOrder - right.sortOrder))

const photoUrl = ref('')
let photoRequest = 0

watch(() => [props.person.id, props.person.photoStorageKey] as const, async ([personId, photoStorageKey]) => {
  const request = ++photoRequest
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value)
  photoUrl.value = ''
  if (!photoStorageKey) return

  try {
    const photo = await personApi.getPhoto(personId)
    if (request === photoRequest) photoUrl.value = URL.createObjectURL(photo)
  } catch {
    if (request === photoRequest) photoUrl.value = ''
  }
}, { immediate: true })

onBeforeUnmount(() => {
  photoRequest++
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value)
})
</script>

<style scoped>
.cv-document { width: 100%; min-height: 620px; padding: clamp(22px, 4vw, 42px); background: #fff; color: #27362e; box-shadow: 0 2px 12px rgb(27 48 36 / 10%); overflow-wrap: anywhere; }
.document-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; padding-bottom: 18px; border-bottom: 3px solid #347463; }
.document-title { min-width: 0; }
.document-title h2 { margin: 0; color: #20382e; font: 700 clamp(22px, 3vw, 31px)/1.1 'Manrope', sans-serif; overflow-wrap: anywhere; }
.document-position { margin: 6px 0 0; color: #647369; font-size: 11px; }
.document-contacts { display: grid; max-width: 42%; gap: 4px; justify-items: end; color: #53645a; font-size: 9px; text-align: right; }
.document-photo { display: block; height: auto; object-fit: cover; border: 2px solid #fff; outline: 1px solid #8b9b90; }
.document-contacts span, .document-rail span { overflow-wrap: anywhere; }
.document-body { padding-top: 21px; }
.document-footer { margin-top: 20px; color: #88948c; font-size: 8px; text-align: right; text-transform: uppercase; }
.cv-document--classic { color: #282828; font-family: Georgia, 'Times New Roman', serif; }
.cv-document--classic .document-header { align-items: center; flex-direction: column; border-bottom: 1px solid #696969; text-align: center; }
.cv-document--classic .document-title h2 { color: #222; font: 700 28px/1.2 Georgia, 'Times New Roman', serif; }
.cv-document--classic .document-position { color: #555; font-family: Georgia, 'Times New Roman', serif; }
.cv-document--classic .document-photo--classic { width: 19%; max-width: 104px; aspect-ratio: 4 / 5; align-self: flex-start; margin-top: 12px; }
.cv-document--classic .document-contacts { display: flex; max-width: none; flex-wrap: wrap; justify-content: center; margin-top: 10px; text-align: center; }
.cv-document--modern .document-header { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 2fr) minmax(0, 1fr); align-items: center; }
.cv-document--modern .document-title { grid-column: 2; grid-row: 1; text-align: center; }
.cv-document--modern .document-photo--modern { grid-column: 1; grid-row: 1; width: 55%; max-width: 100px; aspect-ratio: 4 / 5; justify-self: start; }
.cv-document--modern .document-contacts { grid-column: 3; grid-row: 1; max-width: 100%; }
.cv-document--minimal { padding: clamp(18px, 3vw, 30px); border-top: 5px solid #596f62; }
.cv-document--minimal .document-header { padding-bottom: 13px; border: 0; }
.cv-document--minimal .document-title h2 { color: #28352d; font-size: 25px; }
.cv-document--minimal .document-body { display: grid; grid-template-columns: minmax(100px, 0.28fr) minmax(0, 1fr); gap: 20px; padding-top: 8px; }
.document-rail { display: grid; align-content: start; gap: 7px; min-width: 0; padding-right: 13px; border-right: 1px solid #dce4de; color: #56675c; font-size: 9px; }
.document-rail .document-position { margin: 0 0 7px; }
.document-rail .document-photo--minimal { width: 72%; max-width: 90px; aspect-ratio: 4 / 5; margin: 0 0 7px; }
.document-rail h3 { margin: 0; color: #334d3f; font-size: 9px; text-transform: uppercase; }
.cv-document--minimal .document-footer { margin-top: 14px; }
@media (max-width: 560px) {
  .cv-document { min-height: 480px; padding: 20px 16px; }
  .document-header { align-items: flex-start; flex-direction: column; gap: 9px; }
  .document-contacts { max-width: 100%; justify-items: start; text-align: left; }
  .cv-document--modern .document-header { grid-template-columns: 64px minmax(0, 1fr) 64px; gap: 4px; }
  .cv-document--modern .document-photo--modern { width: 58px; }
  .cv-document--modern .document-contacts { grid-column: 1 / -1; grid-row: 2; justify-items: center; max-width: 100%; text-align: center; }
  .cv-document--classic .document-contacts { justify-content: center; }
  .cv-document--minimal .document-body { grid-template-columns: minmax(0, 1fr); gap: 14px; }
  .document-rail { grid-template-columns: repeat(2, minmax(0, 1fr)); padding: 0 0 12px; border-right: 0; border-bottom: 1px solid #dce4de; }
  .document-rail .document-position { grid-column: 1 / -1; }
  .document-rail h3 { grid-column: 1 / -1; }
}
</style>