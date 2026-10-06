<template>
  <main class="workspace-view">
    <div class="page-heading">
      <div>
        <div class="eyebrow">{{ translate(isPersonView ? 'workspace.personEyebrow' : 'workspace.libraryEyebrow') }}</div>
        <h1>{{ isPersonView ? personName || translate('navigation.personCvs') : translate('navigation.cvLibrary') }}</h1>
        <p>{{ translate(isPersonView ? 'workspace.personDescription' : 'workspace.libraryDescription') }}</p>
      </div>
      <div class="workspace-heading-actions">
        <v-btn variant="outlined" prepend-icon="mdi-file-upload-outline" @click="openDocumentImport">{{ translate('workspace.importDocument') }}</v-btn>
        <v-btn variant="outlined" prepend-icon="mdi-code-json" @click="openImport">{{ translate('workspace.importJson') }}</v-btn>
        <v-btn color="primary" prepend-icon="mdi-plus" rounded="lg" :to="createRoute">{{ translate('workspace.createCv') }}</v-btn>
      </div>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>

    <div class="directory-toolbar">
      <v-text-field
        v-model="search"
        :aria-label="translate('workspace.search')"
        density="compact"
        hide-details
        :label="translate('workspace.search')"
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
        <v-chip size="small" :color="statusColor(item.status)" variant="tonal">{{ translate(`cvForm.${item.status.toLowerCase()}`) }}</v-chip>
      </template>
      <template #item.tags="{ item }">
        <div class="cv-tag-list">
          <v-chip v-for="tag in item.tags" :key="tag" size="small" variant="outlined">{{ tag }}</v-chip>
          <span v-if="!item.tags.length" class="muted-cell">{{ translate('workspace.none') }}</span>
        </div>
      </template>
      <template #item.updatedAt="{ item }">
        <span class="muted-cell">{{ formatDate(item.updatedAt) }}</span>
      </template>
      <template #item.actions="{ item }">
        <div class="row-actions">
          <v-tooltip :text="translate('workspace.editContent')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('workspace.editContentFor', { name: item.name })" icon="mdi-text-box-edit-outline" size="small" variant="text" :to="`/cvs/${item.id}/content`" />
            </template>
          </v-tooltip>
          <v-tooltip :text="translate('workspace.duplicate')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('workspace.duplicateNamed', { name: item.name })" icon="mdi-content-copy" size="small" variant="text" @click="openDuplicate(item)" />
            </template>
          </v-tooltip>
          <v-tooltip :text="translate('workspace.versions')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('workspace.viewVersionsFor', { name: item.name })" icon="mdi-history" size="small" variant="text" :to="`/cvs/${item.id}/versions`" />
            </template>
          </v-tooltip>
          <v-tooltip :text="translate('workspace.exportPdf')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('workspace.exportNamed', { name: item.name, format: 'PDF' })" :disabled="Boolean(exportingCvId)" :loading="exportingCvId === item.id && exportingFormat === 'pdf'" icon="mdi-file-pdf-box" size="small" variant="text" @click="exportCv(item, 'pdf')" />
            </template>
          </v-tooltip>
          <v-tooltip :text="translate('workspace.exportDocx')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('workspace.exportNamed', { name: item.name, format: 'DOCX' })" :disabled="Boolean(exportingCvId)" :loading="exportingCvId === item.id && exportingFormat === 'docx'" icon="mdi-file-word-box" size="small" variant="text" @click="exportCv(item, 'docx')" />
            </template>
          </v-tooltip>
          <v-tooltip :text="translate('workspace.exportJson')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('workspace.exportNamed', { name: item.name, format: 'JSON' })" :disabled="Boolean(exportingCvId)" :loading="exportingCvId === item.id && exportingFormat === 'json'" icon="mdi-code-json" size="small" variant="text" @click="exportCv(item, 'json')" />
            </template>
          </v-tooltip>
          <v-tooltip :text="translate('workspace.editCv')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('workspace.editNamed', { name: item.name })" icon="mdi-pencil-outline" size="small" variant="text" :to="`/cvs/${item.id}/edit`" />
            </template>
          </v-tooltip>
          <v-tooltip :text="translate('workspace.deleteCv')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('workspace.deleteNamed', { name: item.name })" color="error" icon="mdi-trash-can-outline" size="small" variant="text" @click="openDelete(item)" />
            </template>
          </v-tooltip>
        </div>
      </template>
      <template #no-data>
        <div class="people-empty cv-empty">
          <v-icon icon="mdi-file-document-plus-outline" size="30" />
          <h2>{{ translate(store.cvs.length ? 'workspace.noMatchingCvs' : 'workspace.noCvs') }}</h2>
          <p v-if="store.cvs.length">{{ translate('workspace.noSearchResults', { search: search.trim() }) }}</p>
          <p v-else>{{ translate(isPersonView ? 'workspace.createFirstPersonCv' : 'workspace.createLibraryCv') }}</p>
          <v-btn v-if="!store.cvs.length" color="primary" prepend-icon="mdi-plus" :to="createRoute">{{ translate('workspace.createCv') }}</v-btn>
        </div>
      </template>
    </v-data-table>

    <v-dialog v-model="deleteDialog" max-width="430">
      <v-card>
        <v-card-title class="dialog-title">{{ translate('workspace.deleteTitle') }}</v-card-title>
        <v-card-text>{{ translate('workspace.deleteConfirm', { name: selectedCv?.name ?? '' }) }}</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">{{ translate('workspace.cancel') }}</v-btn>
          <v-btn color="error" :loading="deleting" @click="confirmDelete">{{ translate('workspace.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="duplicateDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">{{ translate('workspace.duplicateTitle') }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="duplicateName" :label="translate('workspace.copyName')" maxlength="255" variant="outlined" autofocus />
          <v-select v-model="duplicatePersonId" :items="duplicatePersonOptions" :label="translate('workspace.destinationPerson')" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="duplicateDialog = false">{{ translate('workspace.cancel') }}</v-btn>
          <v-btn color="primary" :loading="duplicating" :disabled="!duplicateName.trim() || !duplicatePersonId" @click="confirmDuplicate">{{ translate('workspace.createCopy') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="importDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">{{ translate('workspace.importTitle') }}</v-card-title>
        <v-card-text>
          <v-select
            v-if="!personId"
            v-model="importPersonId"
            :items="personOptions"
            item-title="name"
            item-value="id"
            :label="translate('workspace.personLabel')"
            variant="outlined"
            :loading="personStore.loading"
          />
          <v-file-input
            v-model="importFile"
            accept=".json,application/json"
            :label="translate('workspace.jsonFile')"
            prepend-icon="mdi-code-json"
            show-size
            variant="outlined"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="importDialog = false">{{ translate('workspace.cancel') }}</v-btn>
          <v-btn color="primary" :loading="importing" :disabled="!canImport" @click="confirmImport">{{ translate('workspace.import') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="documentImportDialog" max-width="760">
      <v-card>
        <v-card-title class="dialog-title">{{ translate('workspace.importDocumentTitle') }}</v-card-title>
        <v-card-text>
          <v-select
            v-if="!personId"
            v-model="documentPersonId"
            :items="personOptions"
            item-title="name"
            item-value="id"
            :label="translate('workspace.personLabel')"
            variant="outlined"
            :loading="personStore.loading"
          />
          <v-file-input
            v-model="documentFile"
            accept=".pdf,.html,.htm,application/pdf,text/html,application/xhtml+xml"
            :label="translate('workspace.documentFile')"
            prepend-icon="mdi-file-document-outline"
            show-size
            variant="outlined"
          />
          <v-alert v-if="documentError" class="mb-4" type="error" variant="tonal">
            {{ documentError }}
          </v-alert>
          <v-btn
            v-if="!documentImportResponse || documentImportResponse.status === 'FAILED'"
            color="primary"
            prepend-icon="mdi-text-box-search-outline"
            :loading="documentImporting"
            :disabled="!documentFileValue || !(personId || documentPersonId)"
            @click="uploadDocument"
          >
            {{ translate('workspace.analyzeDocument') }}
          </v-btn>

          <template v-if="documentImportResponse?.status === 'NEEDS_REVIEW' && documentImportResponse.result">
            <v-alert v-for="(warning, index) in documentImportResponse.result.warnings" :key="index" class="mb-2" type="warning" variant="tonal">
              {{ warning }}
            </v-alert>
            <section class="document-import-preview">
              <h3>{{ translate('workspace.extractedDetails') }}</h3>
              <div class="document-import-grid">
                <div v-if="valueOf(documentImportResponse.result.personalData?.firstName) || valueOf(documentImportResponse.result.personalData?.lastName)">
                  <strong>{{ translate('personForm.firstName') }} / {{ translate('personForm.lastName') }}</strong>
                  <span>{{ [valueOf(documentImportResponse.result.personalData?.firstName), valueOf(documentImportResponse.result.personalData?.lastName)].filter(Boolean).join(' ') }}</span>
                </div>
                <div v-if="valueOf(documentImportResponse.result.personalData?.email)">
                  <strong>{{ translate('personForm.email') }}</strong>
                  <span>{{ valueOf(documentImportResponse.result.personalData?.email) }}</span>
                </div>
                <div v-if="valueOf(documentImportResponse.result.personalData?.phone)">
                  <strong>{{ translate('personForm.phone') }}</strong>
                  <span>{{ valueOf(documentImportResponse.result.personalData?.phone) }}</span>
                </div>
                <div v-if="valueOf(documentImportResponse.result.personalData?.location)">
                  <strong>{{ translate('personForm.location') }}</strong>
                  <span>{{ valueOf(documentImportResponse.result.personalData?.location) }}</span>
                </div>
              </div>
              <div v-if="valueOf(documentImportResponse.result.professionalSummary)" class="document-import-section">
                <h4>{{ translate('workspace.summary') }}</h4>
                <p>{{ valueOf(documentImportResponse.result.professionalSummary) }}</p>
              </div>
              <div v-if="documentImportResponse.result.employment.length" class="document-import-section">
                <h4>{{ translate('workspace.experience') }} ({{ documentImportResponse.result.employment.length }})</h4>
                <div v-for="(entry, index) in documentImportResponse.result.employment" :key="index" class="document-import-item">
                  <strong>{{ [valueOf(entry.position), valueOf(entry.company)].filter(Boolean).join(' · ') || translate('workspace.untitledEntry') }}</strong>
                  <span>{{ [valueOf(entry.startDate), valueOf(entry.endDate), valueOf(entry.location)].filter(Boolean).join(' · ') }}</span>
                </div>
              </div>
              <div v-if="documentImportResponse.result.education.length" class="document-import-section">
                <h4>{{ translate('workspace.education') }} ({{ documentImportResponse.result.education.length }})</h4>
                <div v-for="(entry, index) in documentImportResponse.result.education" :key="index" class="document-import-item">
                  <strong>{{ [valueOf(entry.degree), valueOf(entry.fieldOfStudy), valueOf(entry.institution)].filter(Boolean).join(' · ') || translate('workspace.untitledEntry') }}</strong>
                  <span>{{ [valueOf(entry.startDate), valueOf(entry.endDate)].filter(Boolean).join(' – ') }}</span>
                </div>
              </div>
              <div v-if="documentImportResponse.result.projects.length" class="document-import-section">
                <h4>{{ translate('workspace.projects') }} ({{ documentImportResponse.result.projects.length }})</h4>
                <div v-for="(entry, index) in documentImportResponse.result.projects" :key="index" class="document-import-item">
                  <strong>{{ valueOf(entry.projectName) || translate('workspace.untitledEntry') }}</strong>
                  <span>{{ valueOf(entry.projectDescription) }}</span>
                </div>
              </div>
              <div v-if="documentImportResponse.result.skills.length" class="document-import-section">
                <h4>{{ translate('workspace.skills') }} ({{ documentImportResponse.result.skills.length }})</h4>
                <div class="cv-tag-list">
                  <v-chip v-for="(skill, index) in documentImportResponse.result.skills" :key="index" size="small" variant="outlined">
                    {{ valueOf(skill.name) || skill.canonicalName }}
                  </v-chip>
                </div>
              </div>
              <div v-if="documentImportResponse.result.languages.length" class="document-import-section">
                <h4>{{ translate('workspace.languages') }} ({{ documentImportResponse.result.languages.length }})</h4>
                <div class="cv-tag-list">
                  <v-chip v-for="(entry, index) in documentImportResponse.result.languages" :key="index" size="small" variant="outlined">
                    {{ [valueOf(entry.name), valueOf(entry.proficiency)].filter(Boolean).join(' · ') }}
                  </v-chip>
                </div>
              </div>
            </section>

            <v-divider class="my-4" />
            <h3 class="mb-4">{{ translate('workspace.saveImportedCv') }}</h3>
            <v-text-field v-model="documentCvName" :label="translate('cvForm.name')" maxlength="255" variant="outlined" />
            <div class="document-import-grid">
              <v-text-field v-model="documentLanguage" :label="translate('workspace.language')" maxlength="10" variant="outlined" />
              <v-select v-model="documentStatus" :items="documentStatusOptions" :label="translate('workspace.status')" variant="outlined" />
            </div>
            <v-combobox v-model="documentTags" :label="translate('workspace.tags')" multiple chips closable-chips variant="outlined" />
          </template>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" :disabled="documentImporting || documentApproving" @click="documentImportDialog = false">{{ translate('workspace.cancel') }}</v-btn>
          <v-btn
            v-if="documentImportResponse?.status === 'NEEDS_REVIEW'"
            color="primary"
            prepend-icon="mdi-check"
            :loading="documentApproving"
            :disabled="!documentCvName.trim() || !documentLanguage.trim() || !(personId || documentPersonId)"
            @click="approveDocumentImport"
          >
            {{ translate('workspace.createImportedCv') }}
          </v-btn>
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
import type { Cv, CvDocumentImportResponse, CvStatus, CvVersionSnapshot, ExtractedValue } from '../shared/api/cvTypes'
import { matchesCvSearch } from '../shared/utils/cvSearch'
import { locale, translate } from '../shared/i18n'

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
    ? translate('workspace.countFiltered', { count, total })
    : translate('workspace.count', { count })
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
const documentImportDialog = ref(false)
const documentImporting = ref(false)
const documentApproving = ref(false)
const documentPersonId = ref('')
const documentFile = ref<File | File[] | null>(null)
const documentImportResponse = ref<CvDocumentImportResponse | null>(null)
const documentError = ref('')
const documentCvName = ref('')
const documentLanguage = ref('en')
const documentStatus = ref<CvStatus>('DRAFT')
const documentTags = ref<string[]>([])
const selectedCv = ref<Cv | null>(null)
const cvToDuplicate = ref<Cv | null>(null)
const personOptions = computed(() => personStore.people.map((person) => ({
  id: person.id,
  name: `${person.firstName} ${person.lastName}`,
})))
const canImport = computed(() => Boolean(importFile.value && (personId.value || importPersonId.value)))
const documentStatusOptions = computed(() => (['DRAFT', 'ACTIVE', 'ARCHIVED'] as CvStatus[]).map((status) => ({
  title: translate(`cvForm.${status.toLowerCase()}`),
  value: status,
})))
const headers = computed(() => [
  { title: translate('workspace.cv'), key: 'name' },
  ...(!isPersonView.value ? [{ title: translate('workspace.person'), key: 'personName' }] : []),
  { title: translate('workspace.language'), key: 'language' },
  { title: translate('workspace.tags'), key: 'tags', sortable: false },
  { title: translate('workspace.status'), key: 'status' },
  { title: translate('workspace.updated'), key: 'updatedAt' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])
const duplicatePersonOptions = computed(() => personStore.people.map((person) => ({
  title: `${person.firstName} ${person.lastName}`,
  value: person.id,
})))

watch(personId, () => loadCvs(), { immediate: true })

watch(documentFile, () => {
  documentImportResponse.value = null
  documentCvName.value = ''
})

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
    error.value = cause instanceof Error ? cause.message : translate('workspace.loadError')
  }
}

function statusColor(status: CvStatus) {
  return status === 'ACTIVE' ? 'success' : status === 'ARCHIVED' ? 'secondary' : 'warning'
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(locale.value === 'lv' ? 'lv-LV' : 'en-US', { dateStyle: 'medium' }).format(new Date(value))
}

async function exportCv(cv: Cv, format: 'pdf' | 'docx' | 'json') {
  if (exportingCvId.value) return
  exportingCvId.value = cv.id
  exportingFormat.value = format
  error.value = ''
  try {
    const version = await cvApi.createVersion(cv.id, translate('workspace.exportVersionDescription'))
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
    error.value = cause instanceof Error ? cause.message : translate('workspace.exportError', { format: format.toUpperCase() })
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
    error.value = cause instanceof Error ? cause.message : translate('workspace.duplicateError')
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
      error.value = cause instanceof Error ? cause.message : translate('workspace.loadPeopleError')
      return
    }
  }
  importDialog.value = true
}

async function openDocumentImport() {
  error.value = ''
  documentError.value = ''
  documentFile.value = null
  documentImportResponse.value = null
  documentCvName.value = ''
  documentLanguage.value = 'en'
  documentStatus.value = 'DRAFT'
  documentTags.value = []
  documentPersonId.value = personId.value ?? ''
  if (!personId.value) {
    try {
      await personStore.fetchPeople()
      documentPersonId.value = personOptions.value[0]?.id ?? ''
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : translate('workspace.loadPeopleError')
      return
    }
  }
  documentImportDialog.value = true
}

const documentFileValue = computed(() => Array.isArray(documentFile.value) ? documentFile.value[0] : documentFile.value)

function valueOf(value: ExtractedValue<string> | null | undefined) {
  return value?.value?.trim() ?? ''
}

async function uploadDocument() {
  if (!documentFileValue.value) return
  documentImporting.value = true
  documentError.value = ''
  try {
    const response = await cvApi.startDocumentImport(documentFileValue.value)
    documentImportResponse.value = response
    if (response.status === 'FAILED') {
      documentError.value = response.errorMessage || translate('workspace.documentImportError')
    } else {
      documentCvName.value = response.fileName.replace(/\.(pdf|html?)$/i, '')
    }
  } catch (cause) {
    documentError.value = cause instanceof Error ? cause.message : translate('workspace.documentImportError')
  } finally {
    documentImporting.value = false
  }
}

async function approveDocumentImport() {
  const importId = documentImportResponse.value?.importId
  const destinationPersonId = personId.value ?? documentPersonId.value
  if (!importId || !destinationPersonId || !documentCvName.value.trim()) return
  documentApproving.value = true
  documentError.value = ''
  try {
    const response = await cvApi.approveDocumentImport(importId, {
      personId: destinationPersonId,
      name: documentCvName.value.trim(),
      language: documentLanguage.value.trim(),
      status: documentStatus.value,
      tags: documentTags.value,
    })
    if (!response.cvId) throw new Error(translate('workspace.documentImportError'))
    await store.fetchCvs(personId.value)
    documentImportDialog.value = false
    await router.push(`/cvs/${response.cvId}/content`)
  } catch (cause) {
    documentError.value = cause instanceof Error ? cause.message : translate('workspace.documentImportError')
  } finally {
    documentApproving.value = false
  }
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
    error.value = cause instanceof Error ? cause.message : translate('workspace.importError')
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
    error.value = cause instanceof Error ? cause.message : translate('workspace.deleteError')
  } finally {
    deleting.value = false
  }
}
</script>