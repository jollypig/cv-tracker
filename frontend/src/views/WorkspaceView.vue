<template>
  <main class="workspace-view">
    <div class="page-heading">
      <div>
        <div class="eyebrow">{{ isPersonView ? 'PERSON CVs' : 'YOUR CAREER DOCUMENTS' }}</div>
        <h1>{{ isPersonView ? personName || 'CVs' : 'CV library' }}</h1>
        <p>{{ isPersonView ? 'CVs connected to this profile.' : 'Every CV across your workspace.' }}</p>
      </div>
      <div class="workspace-heading-actions">
        <v-btn variant="outlined" prepend-icon="mdi-code-json" @click="openImport">Import JSON</v-btn>
        <v-btn color="primary" prepend-icon="mdi-plus" rounded="lg" :to="createRoute">Create CV</v-btn>
      </div>
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
      <span class="directory-count">{{ directoryCount }}</span>
    </div>

    <v-progress-linear v-if="store.loading" color="primary" indeterminate />

    <v-data-table
      v-else
      class="people-table cv-table"
      :headers="headers"
      :items="filteredCvs"
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
      <template #item.tags="{ item }">
        <div class="cv-tag-list">
          <v-chip v-for="tag in item.tags" :key="tag" size="small" variant="outlined">{{ tag }}</v-chip>
          <span v-if="!item.tags.length" class="muted-cell">None</span>
        </div>
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
              <v-btn v-bind="props" :aria-label="`Export ${item.name} as PDF`" :disabled="Boolean(exportingCvId)" :loading="exportingCvId === item.id && exportingFormat === 'pdf'" icon="mdi-file-pdf-box" size="small" variant="text" @click="exportCv(item, 'pdf')" />
            </template>
          </v-tooltip>
          <v-tooltip text="Export current CV as DOCX; saves a version">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Export ${item.name} as DOCX`" :disabled="Boolean(exportingCvId)" :loading="exportingCvId === item.id && exportingFormat === 'docx'" icon="mdi-file-word-box" size="small" variant="text" @click="exportCv(item, 'docx')" />
            </template>
          </v-tooltip>
          <v-tooltip text="Export current CV as JSON; saves a version">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Export ${item.name} as JSON`" :disabled="Boolean(exportingCvId)" :loading="exportingCvId === item.id && exportingFormat === 'json'" icon="mdi-code-json" size="small" variant="text" @click="exportCv(item, 'json')" />
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
          <p v-if="store.cvs.length">No CVs match “{{ search.trim() }}”.</p>
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
          <v-select v-model="duplicatePersonId" :items="duplicatePersonOptions" label="Destination person" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="duplicateDialog = false">Cancel</v-btn>
          <v-btn color="primary" :loading="duplicating" :disabled="!duplicateName.trim() || !duplicatePersonId" @click="confirmDuplicate">Create copy</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="importDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">Import CV JSON</v-card-title>
        <v-card-text>
          <v-select
            v-if="!personId"
            v-model="importPersonId"
            :items="personOptions"
            item-title="name"
            item-value="id"
            label="Person"
            variant="outlined"
            :loading="personStore.loading"
          />
          <v-file-input
            v-model="importFile"
            accept=".json,application/json"
            label="JSON file"
            prepend-icon="mdi-code-json"
            show-size
            variant="outlined"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="importDialog = false">Cancel</v-btn>
          <v-btn color="primary" :loading="importing" :disabled="!canImport" @click="confirmImport">Import</v-btn>
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
import type { Cv, CvStatus, CvVersionSnapshot } from '../shared/api/cvTypes'
import { matchesCvSearch } from '../shared/utils/cvSearch'

const route = useRoute()
const router = useRouter()
const store = useCvStore()
const personStore = usePersonStore()
const personId = computed(() => typeof route.params.personId === 'string' ? route.params.personId : undefined)
const isPersonView = computed(() => Boolean(personId.value))
const createRoute = computed(() => personId.value ? `/people/${personId.value}/cvs/new` : '/cvs/new')
const personName = ref('')
const search = ref('')
const filteredCvs = computed(() => store.cvs.filter((cv) => matchesCvSearch(cv, search.value)))
const directoryCount = computed(() => {
  const count = filteredCvs.value.length
  const total = store.cvs.length
  return search.value.trim()
    ? `${count} of ${total} ${total === 1 ? 'CV' : 'CVs'}`
    : `${count} ${count === 1 ? 'CV' : 'CVs'}`
})
const error = ref('')
const deleteDialog = ref(false)
const deleting = ref(false)
const duplicateDialog = ref(false)
const duplicateName = ref('')
const duplicatePersonId = ref('')
const duplicating = ref(false)
const exportingCvId = ref('')
const exportingFormat = ref<'pdf' | 'docx' | 'json' | null>(null)
const importDialog = ref(false)
const importing = ref(false)
const importPersonId = ref('')
const importFile = ref<File | File[] | null>(null)
const selectedCv = ref<Cv | null>(null)
const cvToDuplicate = ref<Cv | null>(null)
const personOptions = computed(() => personStore.people.map((person) => ({
  id: person.id,
  name: `${person.firstName} ${person.lastName}`,
})))
const canImport = computed(() => Boolean(importFile.value && (personId.value || importPersonId.value)))
const headers = computed(() => [
  { title: 'CV', key: 'name' },
  ...(!isPersonView.value ? [{ title: 'Person', key: 'personName' }] : []),
  { title: 'Language', key: 'language' },
  { title: 'Tags', key: 'tags', sortable: false },
  { title: 'Status', key: 'status' },
  { title: 'Updated', key: 'updatedAt' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])
const duplicatePersonOptions = computed(() => personStore.people.map((person) => ({
  title: `${person.firstName} ${person.lastName}`,
  value: person.id,
})))

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
    await Promise.all([store.fetchCvs(personId.value), personStore.fetchPeople()])
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

async function exportCv(cv: Cv, format: 'pdf' | 'docx' | 'json') {
  if (exportingCvId.value) return
  exportingCvId.value = cv.id
  exportingFormat.value = format
  error.value = ''
  try {
    const version = await cvApi.createVersion(cv.id, 'Exported from CV library')
    const file = await cvApi.exportVersion(version.id, format)
    const url = URL.createObjectURL(file.content)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(url), 0)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : `Unable to export this CV as ${format.toUpperCase()}.`
  } finally {
    exportingCvId.value = ''
    exportingFormat.value = null
  }
}

function openDelete(cv: Cv) {
  selectedCv.value = cv
  deleteDialog.value = true
}

function openDuplicate(cv: Cv) {
  cvToDuplicate.value = cv
  duplicateName.value = `${cv.name} (Copy)`
  duplicatePersonId.value = cv.personId
  duplicateDialog.value = true
}

async function confirmDuplicate() {
  if (!cvToDuplicate.value || !duplicateName.value.trim()) return
  duplicating.value = true
  error.value = ''
  try {
    const copy = await cvApi.duplicate(cvToDuplicate.value.id, duplicateName.value.trim(), duplicatePersonId.value)
    await router.push(`/cvs/${copy.id}/content`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to duplicate this CV.'
  } finally {
    duplicating.value = false
    duplicateDialog.value = false
    cvToDuplicate.value = null
  }
}

async function openImport() {
  error.value = ''
  importFile.value = null
  importPersonId.value = personId.value ?? ''
  if (!personId.value) {
    try {
      await personStore.fetchPeople()
      importPersonId.value = personOptions.value[0]?.id ?? ''
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : 'Unable to load people for import.'
      return
    }
  }
  importDialog.value = true
}

async function confirmImport() {
  const file = Array.isArray(importFile.value) ? importFile.value[0] : importFile.value
  const destinationPersonId = personId.value ?? importPersonId.value
  if (!file || !destinationPersonId) return

  importing.value = true
  error.value = ''
  try {
    const snapshot = JSON.parse(await file.text()) as CvVersionSnapshot
    const importedCv = await cvApi.importSnapshot(destinationPersonId, snapshot)
    await store.fetchCvs(personId.value)
    importDialog.value = false
    await router.push(`/cvs/${importedCv.id}/content`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to import this CV.'
  } finally {
    importing.value = false
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