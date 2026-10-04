<template>
  <main class="cv-version-diff-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">{{ translate('diff.eyebrow') }}</div>
        <h1>{{ cv?.name ?? 'Compare versions' }}</h1>
        <p v-if="diff">v{{ diff.fromVersion.versionNumber }} to v{{ diff.toVersion.versionNumber }}</p>
      </div>
      <div class="content-heading-actions">
        <v-btn variant="text" prepend-icon="mdi-history" :to="`/cvs/${cvId}/versions`">{{ translate('diff.allVersions') }}</v-btn>
        <v-btn v-if="diff" variant="text" prepend-icon="mdi-open-in-new" :to="`/cvs/${cvId}/versions/${diff.toVersion.versionNumber}`">{{ translate('diff.viewVersion', { version: diff.toVersion.versionNumber }) }}</v-btn>
      </div>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal">{{ error }}</v-alert>
    <v-progress-linear v-if="loading" color="primary" indeterminate />

    <template v-else-if="diff">
      <section class="diff-summary" :aria-label="translate('diff.summary')">
        <div>
          <span>{{ translate('diff.from') }}</span>
          <strong>v{{ diff.fromVersion.versionNumber }}</strong>
          <small>{{ diff.fromVersion.description || formatDate(diff.fromVersion.createdAt) }}</small>
        </div>
        <v-icon icon="mdi-arrow-right" aria-hidden="true" />
        <div>
          <span>{{ translate('diff.to') }}</span>
          <strong>v{{ diff.toVersion.versionNumber }}</strong>
          <small>{{ diff.toVersion.description || formatDate(diff.toVersion.createdAt) }}</small>
        </div>
        <div class="diff-counts">
          <span>{{ count('ADDED') }} {{ translate('diff.added') }}</span>
          <span>{{ count('REMOVED') }} {{ translate('diff.removed') }}</span>
          <span>{{ count('MODIFIED') }} {{ translate('diff.modified') }}</span>
        </div>
      </section>

      <section v-if="diff.changes.length" class="diff-change-list" :aria-label="translate('diff.changedFields')">
        <article v-for="(change, index) in diff.changes" :key="`${change.path}-${index}`" class="diff-change" :class="`change-${change.type.toLowerCase()}`">
          <div class="change-heading">
            <code>{{ change.path }}</code>
            <v-chip size="small" label :color="changeColor(change.type)">{{ translate(`diff.${change.type.toLowerCase()}`) }}</v-chip>
          </div>
          <div v-if="change.type !== 'ADDED'" class="change-value old-value">
            <span>{{ translate('diff.before') }}</span><pre>{{ formatValue(change.oldValue) }}</pre>
          </div>
          <div v-if="change.type !== 'REMOVED'" class="change-value new-value">
            <span>{{ translate('diff.after') }}</span><pre>{{ formatValue(change.newValue) }}</pre>
          </div>
        </article>
      </section>
      <div v-else class="diff-empty">
        <v-icon icon="mdi-check-circle-outline" size="28" />
        <h2>{{ translate('diff.emptyTitle') }}</h2>
      </div>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import cvApi from '../shared/api/cvApi'
import type { Cv, CvVersionChangeType, CvVersionDiff } from '../shared/api/cvTypes'
import { locale, translate } from '../shared/i18n'

const route = useRoute()
const cvId = typeof route.params.id === 'string' ? route.params.id : ''
const fromVersion = computed(() => Number(route.query.from))
const toVersion = computed(() => Number(route.query.to))
const cv = ref<Cv | null>(null)
const diff = ref<CvVersionDiff | null>(null)
const loading = ref(true)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (!Number.isInteger(fromVersion.value) || !Number.isInteger(toVersion.value) || fromVersion.value < 1 || toVersion.value < 1) {
      throw new Error(translate('diff.invalidVersions'))
    }
    const [cvData, diffData] = await Promise.all([
      cvApi.get(cvId), cvApi.getVersionDiff(cvId, fromVersion.value, toVersion.value),
    ])
    cv.value = cvData
    diff.value = diffData
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('diff.loadError')
  } finally {
    loading.value = false
  }
}

function count(type: CvVersionChangeType) {
  return diff.value?.changes.filter((change) => change.type === type).length ?? 0
}

function changeColor(type: CvVersionChangeType) {
  return type === 'ADDED' ? 'success' : type === 'REMOVED' ? 'error' : 'warning'
}

function formatValue(value: unknown) {
  if (value === null || value === undefined) return translate('diff.notSet')
  return typeof value === 'string' ? value : JSON.stringify(value, null, 2)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(locale.value === 'lv' ? 'lv-LV' : 'en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
.cv-version-diff-view { max-width: 1100px; margin: 0 auto; }
.diff-summary { display: grid; grid-template-columns: minmax(0, 1fr) 28px minmax(0, 1fr) auto; align-items: center; gap: 20px; padding: 22px 0; border-bottom: 1px solid #dfe4de; }
.diff-summary > div:not(.diff-counts) { display: grid; gap: 4px; }
.diff-summary span, .change-value > span { color: #76847c; font-size: 11px; font-weight: 700; }
.diff-summary strong { color: #263b31; font-size: 18px; }
.diff-summary small { color: #76847c; }
.diff-counts { display: flex; flex-wrap: wrap; gap: 8px 14px; color: #52635a; font-size: 12px; }
.diff-change-list { display: grid; gap: 10px; padding: 18px 0; }
.diff-change { display: grid; grid-template-columns: minmax(180px, .8fr) minmax(0, 1fr) minmax(0, 1fr); gap: 14px; padding: 14px; border-left: 4px solid #a9b5ae; background: #fff; }
.change-added { border-color: #37805b; }
.change-removed { border-color: #bd5146; }
.change-modified { border-color: #bb842e; }
.change-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; min-width: 0; }
.change-heading code { overflow-wrap: anywhere; color: #263b31; font-size: 13px; }
.change-value { min-width: 0; }
.change-value pre { overflow: auto; max-height: 220px; margin: 6px 0 0; color: #34443b; font: 12px/1.55 'Cascadia Code', Consolas, monospace; white-space: pre-wrap; overflow-wrap: anywhere; }
.old-value pre { color: #98433b; }
.new-value pre { color: #26714b; }
.diff-empty { display: grid; justify-items: center; gap: 10px; padding: 72px 16px; color: #52635a; text-align: center; }
.diff-empty h2 { margin: 0; color: #263b31; font: 700 18px/1.4 'Manrope', sans-serif; }
@media (max-width: 760px) {
  .diff-summary { grid-template-columns: minmax(0, 1fr) 22px minmax(0, 1fr); gap: 10px; }
  .diff-counts { grid-column: 1 / -1; }
  .diff-change { grid-template-columns: minmax(0, 1fr); gap: 10px; }
}
</style>