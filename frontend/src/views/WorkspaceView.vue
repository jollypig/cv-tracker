<template>
  <main class="workspace-view">
    <div class="page-heading">
      <div>
        <div class="eyebrow">{{ isPersonView ? 'PERSON CVs' : 'YOUR CAREER DOCUMENTS' }}</div>
        <h1>{{ isPersonView ? personName || 'CVs' : 'CV library' }}</h1>
        <p>{{ isPersonView ? 'CVs connected to this profile.' : 'Every CV across your workspace.' }}</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-plus" rounded="lg" :to="createRoute">Create CV</v-btn>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>

    <div class="directory-toolbar">
      <v-text-field
        v-model="search"
        aria-label="Search CVs"
        density="compact"
        hide-details
        label="Search CVs"
        prepend-inner-icon="mdi-magnify"
        variant="outlined"
        class="directory-search"
      />
      <span class="directory-count">{{ store.cvs.length }} {{ store.cvs.length === 1 ? 'CV' : 'CVs' }}</span>
    </div>

    <v-progress-linear v-if="store.loading" color="primary" indeterminate />

    <v-data-table
      v-else
      class="people-table cv-table"
      :headers="headers"
      :items="store.cvs"
      :search="search"
      item-value="id"
      :items-per-page="10"
    >
      <template #item.name="{ item }">
        <RouterLink class="person-name-link" :to="`/cvs/${item.id}/edit`">{{ item.name }}</RouterLink>
      </template>
      <template #item.personName="{ item }">
        <RouterLink class="person-name-link" :to="`/people/${item.personId}/cvs`">{{ item.personName }}</RouterLink>
      </template>
      <template #item.status="{ item }">
        <v-chip size="small" :color="statusColor(item.status)" variant="tonal">{{ item.status }}</v-chip>
      </template>
      <template #item.updatedAt="{ item }">
        <span class="muted-cell">{{ formatDate(item.updatedAt) }}</span>
      </template>
      <template #item.actions="{ item }">
        <div class="row-actions">
          <v-tooltip text="Edit CV content">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Edit content for ${item.name}`" icon="mdi-text-box-edit-outline" size="small" variant="text" :to="`/cvs/${item.id}/content`" />
            </template>
          </v-tooltip>
          <v-tooltip text="Duplicate CV">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Duplicate ${item.name}`" icon="mdi-content-copy" size="small" variant="text" @click="openDuplicate(item)" />
            </template>
          </v-tooltip>
          <v-tooltip text="Version history">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`View versions for ${item.name}`" icon="mdi-history" size="small" variant="text" :to="`/cvs/${item.id}/versions`" />
            </template>
          </v-tooltip>
          <v-tooltip text="Export current CV as PDF; saves a version">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Export ${item.name} as PDF`" :disabled="Boolean(exportingCvId)" :loading="exportingCvId === item.id" icon="mdi-file-pdf-box" size="small" variant="text" @click="exportCv(item)" />
            </template>
          </v-tooltip>
          <v-tooltip text="Edit CV">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Edit ${item.name}`" icon="mdi-pencil-outline" size="small" variant="text" :to="`/cvs/${item.id}/edit`" />
            </template>
          </v-tooltip>
          <v-tooltip text="Delete CV">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Delete ${item.name}`" color="error" icon="mdi-trash-can-outline" size="small" variant="text" @click="openDelete(item)" />
            </template>
          </v-tooltip>
        </div>
      </template>
      <template #no-data>
        <div class="people-empty cv-empty">
          <v-icon icon="mdi-file-document-plus-outline" size="30" />
          <h2>{{ store.cvs.length ? 'No matching CVs' : 'No CVs yet' }}</h2>
          <p v-if="store.cvs.length">No CVs match “{{ search }}”.</p>
          <p v-else>{{ isPersonView ? 'Create the first CV for this person.' : 'Create a CV to start building your library.' }}</p>
          <v-btn v-if="!store.cvs.length" color="primary" prepend-icon="mdi-plus" :to="createRoute">Create CV</v-btn>
        </div>
      </template>
    </v-data-table>

    <v-dialog v-model="deleteDialog" max-width="430">
      <v-card>
        <v-card-title class="dialog-title">Delete CV?</v-card-title>
        <v-card-text>This will permanently delete “{{ selectedCv?.name }}”.</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">Cancel</v-btn>
          <v-btn color="error" :loading="deleting" @click="confirmDelete">Delete</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="duplicateDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">Duplicate this CV</v-card-title>
        <v-card-text>
          <v-text-field v-model="duplicateName" label="Copy name" maxlength="255" variant="outlined" autofocus />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="duplicateDialog = false">Cancel</v-btn>
          <v-btn color="primary" :loading="duplicating" :disabled="!duplicateName.trim()" @click="confirmDuplicate">Create copy</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import cvApi from '../shared/api/cvApi'
import { useCvStore } from '../shared/stores/cvStore'
import { usePersonStore } from '../shared/stores/personStore'
import type { Cv, CvStatus } from '../shared/api/cvTypes'

const route = useRoute()
const router = useRouter()
const store = useCvStore()
const personStore = usePersonStore()
const personId = computed(() => typeof route.params.personId === 'string' ? route.params.personId : undefined)
const isPersonView = computed(() => Boolean(personId.value))
const createRoute = computed(() => personId.value ? `/people/${personId.value}/cvs/new` : '/cvs/new')
const personName = ref('')
const search = ref('')
const error = ref('')
const deleteDialog = ref(false)
const deleting = ref(false)
const duplicateDialog = ref(false)
const duplicateName = ref('')
const duplicating = ref(false)
const exportingCvId = ref('')
const selectedCv = ref<Cv | null>(null)
const cvToDuplicate = ref<Cv | null>(null)
const headers = computed(() => [
  { title: 'CV', key: 'name' },
  ...(!isPersonView.value ? [{ title: 'Person', key: 'personName' }] : []),
  { title: 'Language', key: 'language' },
  { title: 'Status', key: 'status' },
  { title: 'Updated', key: 'updatedAt' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])

watch(personId, () => loadCvs(), { immediate: true })

async function loadCvs() {
  error.value = ''
  try {
    if (personId.value) {
      const person = await personStore.fetchPerson(personId.value)
      personName.value = `${person.firstName} ${person.lastName}`
    } else {
      personName.value = ''
    }
    await store.fetchCvs(personId.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to load CVs.'
  }
}

function statusColor(status: CvStatus) {
  return status === 'ACTIVE' ? 'success' : status === 'ARCHIVED' ? 'secondary' : 'warning'
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value))
}

async function exportCv(cv: Cv) {
  if (exportingCvId.value) return
  exportingCvId.value = cv.id
  error.value = ''
  try {
    const version = await cvApi.createVersion(cv.id, 'Exported from CV library')
    const file = await cvApi.exportVersionPdf(version.id)
    const url = URL.createObjectURL(file.content)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(url), 0)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to export this CV.'
  } finally {
    exportingCvId.value = ''
  }
}

function openDelete(cv: Cv) {
  selectedCv.value = cv
  deleteDialog.value = true
}

function openDuplicate(cv: Cv) {
  cvToDuplicate.value = cv
  duplicateName.value = `${cv.name} (Copy)`
  duplicateDialog.value = true
}

async function confirmDuplicate() {
  if (!cvToDuplicate.value || !duplicateName.value.trim()) return
  duplicating.value = true
  error.value = ''
  try {
    const copy = await cvApi.duplicate(cvToDuplicate.value.id, duplicateName.value.trim())
    await router.push(`/cvs/${copy.id}/content`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to duplicate this CV.'
  } finally {
    duplicating.value = false
    duplicateDialog.value = false
    cvToDuplicate.value = null
  }
}

async function confirmDelete() {
  if (!selectedCv.value) return
  deleting.value = true
  error.value = ''
  try {
    await store.deleteCv(selectedCv.value.id)
    deleteDialog.value = false
    selectedCv.value = null
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to delete this CV.'
  } finally {
    deleting.value = false
  }
}
</script>