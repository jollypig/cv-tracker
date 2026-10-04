<template>
  <main class="cv-versions-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">{{ translate('versions.eyebrow') }}</div>
        <h1>{{ cv?.name ?? translate('versions.fallbackTitle') }}</h1>
        <p>{{ cv?.personName ?? translate('versions.fallbackDescription') }}</p>
      </div>
      <div class="content-heading-actions">
        <v-btn variant="text" prepend-icon="mdi-content-copy" @click="openDuplicateDialog">{{ translate('versions.duplicate') }}</v-btn>
        <v-btn variant="text" prepend-icon="mdi-text-box-edit-outline" :to="`/cvs/${cvId}/content`">{{ translate('versions.editContent') }}</v-btn>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/cvs/${cvId}/edit`">{{ translate('versions.cvDetails') }}</v-btn>
      </div>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>

    <section class="version-create-row" :aria-label="translate('versions.createLabel')">
      <v-text-field
        v-model="description"
        :label="translate('versions.description')"
        :placeholder="translate('versions.descriptionPlaceholder')"
        maxlength="500"
        counter="500"
        hide-details="auto"
        variant="outlined"
        @keydown.enter.prevent="createVersion"
      />
      <v-btn color="primary" :loading="creating" prepend-icon="mdi-content-save-plus-outline" @click="createVersion">
        {{ translate('versions.save') }}
      </v-btn>
    </section>

    <v-progress-linear v-if="loading" color="primary" indeterminate />
    <section v-else-if="versions.length" class="version-list" :aria-label="translate('navigation.cvVersions')">
      <div class="version-list-heading"><span>{{ translate('versions.version') }}</span><span>{{ translate('versions.descriptionColumn') }}</span><span>{{ translate('versions.created') }}</span><span /></div>
      <article v-for="version in versions" :key="version.id" class="version-list-row">
        <div class="version-number">v{{ version.versionNumber }}</div>
        <div class="version-description">
          {{ version.description || translate('versions.savedSnapshot') }}
          <RouterLink
            v-if="version.parentVersionNumber && version.parentCvId"
            class="version-parent"
            :to="`/cvs/${version.parentCvId}/versions/${version.parentVersionNumber}`"
          >
            {{ version.parentCvId === cvId ? translate('versions.branchOf', { version: `v${version.parentVersionNumber}` }) : translate('versions.sourceBranch', { version: version.parentVersionNumber }) }}
          </RouterLink>
        </div>
        <time class="muted-cell" :datetime="version.createdAt">{{ formatDate(version.createdAt) }}</time>
        <v-btn :to="`/cvs/${cvId}/versions/${version.versionNumber}`" variant="text" append-icon="mdi-arrow-right">
          {{ translate('versions.view') }}
        </v-btn>
      </article>
    </section>
    <div v-else-if="!error" class="version-empty">
      <v-icon icon="mdi-history" size="30" />
      <h2>{{ translate('versions.emptyTitle') }}</h2>
      <p>{{ translate('versions.emptyDescription') }}</p>
    </div>

    <section v-if="versions.length > 1" class="version-compare-row" :aria-label="translate('versions.compareLabel')">
      <v-select v-model="fromVersion" :items="versionOptions" :label="translate('versions.from')" variant="outlined" hide-details />
      <v-icon icon="mdi-arrow-right" aria-hidden="true" />
      <v-select v-model="toVersion" :items="versionOptions" :label="translate('versions.to')" variant="outlined" hide-details />
      <v-btn
        color="primary"
        prepend-icon="mdi-compare-horizontal"
        :disabled="fromVersion === null || toVersion === null || fromVersion === toVersion"
        :to="fromVersion !== null && toVersion !== null ? `/cvs/${cvId}/versions/diff?from=${fromVersion}&to=${toVersion}` : undefined"
      >
        {{ translate('versions.compare') }}
      </v-btn>
    </section>

    <v-dialog v-model="duplicateDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">{{ translate('versions.duplicateTitle') }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="duplicateName" :label="translate('versions.copyName')" maxlength="255" variant="outlined" autofocus />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="duplicateDialog = false">{{ translate('versions.cancel') }}</v-btn>
          <v-btn color="primary" :loading="duplicating" :disabled="!duplicateName.trim()" @click="duplicateCv">{{ translate('versions.createCopy') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import cvApi from '../shared/api/cvApi'
import type { Cv, CvVersion } from '../shared/api/cvTypes'
import { locale, translate } from '../shared/i18n'

const route = useRoute()
const router = useRouter()
const cvId = typeof route.params.id === 'string' ? route.params.id : ''
const cv = ref<Cv | null>(null)
const versions = ref<CvVersion[]>([])
const description = ref('')
const loading = ref(true)
const creating = ref(false)
const error = ref('')
const fromVersion = ref<number | null>(null)
const toVersion = ref<number | null>(null)
const duplicateDialog = ref(false)
const duplicateName = ref('')
const duplicating = ref(false)
const versionOptions = computed(() => versions.value.map((version) => ({
  title: `v${version.versionNumber} · ${version.description || translate('versions.savedSnapshot')}`,
  value: version.versionNumber,
})))

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [cvData, versionData] = await Promise.all([cvApi.get(cvId), cvApi.listVersions(cvId)])
    cv.value = cvData
    versions.value = versionData
    fromVersion.value = versionData[versionData.length - 1]?.versionNumber ?? null
    toVersion.value = versionData[0]?.versionNumber ?? null
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('versions.loadError')
  } finally {
    loading.value = false
  }
}

function openDuplicateDialog() {
  duplicateName.value = `${cv.value?.name ?? 'CV'} (Copy)`
  duplicateDialog.value = true
}

async function duplicateCv() {
  duplicating.value = true
  error.value = ''
  try {
    const copy = await cvApi.duplicate(cvId, duplicateName.value.trim())
    await router.push(`/cvs/${copy.id}/content`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('versions.duplicateError')
  } finally {
    duplicating.value = false
    duplicateDialog.value = false
  }
}

async function createVersion() {
  creating.value = true
  error.value = ''
  try {
    const version = await cvApi.createVersion(cvId, description.value.trim())
    versions.value = [version, ...versions.value]
    description.value = ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('versions.saveError')
  } finally {
    creating.value = false
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(locale.value === 'lv' ? 'lv-LV' : 'en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
.version-parent { display: block; width: fit-content; margin-top: 4px; color: #64756c; font-size: 12px; }
.version-compare-row { display: grid; grid-template-columns: minmax(0, 1fr) 24px minmax(0, 1fr) auto; align-items: center; gap: 14px; margin-top: 24px; padding: 20px 0; border-top: 1px solid #e4e5de; }
@media (max-width: 680px) {
  .version-compare-row { grid-template-columns: minmax(0, 1fr); gap: 8px; }
  .version-compare-row > .v-icon { display: none; }
}
</style>