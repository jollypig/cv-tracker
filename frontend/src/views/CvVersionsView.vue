<template>
  <main class="cv-versions-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">IMMUTABLE HISTORY</div>
        <h1>{{ cv?.name ?? 'CV versions' }}</h1>
        <p>{{ cv?.personName ?? 'Saved snapshots of this CV.' }}</p>
      </div>
      <div class="content-heading-actions">
        <v-btn variant="text" prepend-icon="mdi-text-box-edit-outline" :to="`/cvs/${cvId}/content`">Edit content</v-btn>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/cvs/${cvId}/edit`">CV details</v-btn>
      </div>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>

    <section class="version-create-row" aria-label="Create a version">
      <v-text-field
        v-model="description"
        label="Version description"
        placeholder="For example, Before tailoring for a new role"
        maxlength="500"
        counter="500"
        hide-details="auto"
        variant="outlined"
        @keydown.enter.prevent="createVersion"
      />
      <v-btn color="primary" :loading="creating" prepend-icon="mdi-content-save-plus-outline" @click="createVersion">
        Save version
      </v-btn>
    </section>

    <v-progress-linear v-if="loading" color="primary" indeterminate />
    <section v-else-if="versions.length" class="version-list" aria-label="Version history">
      <div class="version-list-heading"><span>VERSION</span><span>DESCRIPTION</span><span>CREATED</span><span /></div>
      <article v-for="version in versions" :key="version.id" class="version-list-row">
        <div class="version-number">v{{ version.versionNumber }}</div>
        <div class="version-description">{{ version.description || 'Saved snapshot' }}</div>
        <time class="muted-cell" :datetime="version.createdAt">{{ formatDate(version.createdAt) }}</time>
        <v-btn :to="`/cvs/${cvId}/versions/${version.versionNumber}`" variant="text" append-icon="mdi-arrow-right">
          View
        </v-btn>
      </article>
    </section>
    <div v-else-if="!error" class="version-empty">
      <v-icon icon="mdi-history" size="30" />
      <h2>No saved versions</h2>
      <p>Create a snapshot to preserve the current CV content.</p>
    </div>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import cvApi from '../shared/api/cvApi'
import type { Cv, CvVersion } from '../shared/api/cvTypes'

const route = useRoute()
const cvId = typeof route.params.id === 'string' ? route.params.id : ''
const cv = ref<Cv | null>(null)
const versions = ref<CvVersion[]>([])
const description = ref('')
const loading = ref(true)
const creating = ref(false)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [cvData, versionData] = await Promise.all([cvApi.get(cvId), cvApi.listVersions(cvId)])
    cv.value = cvData
    versions.value = versionData
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to load CV versions.'
  } finally {
    loading.value = false
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
    error.value = cause instanceof Error ? cause.message : 'Unable to save this CV version.'
  } finally {
    creating.value = false
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>