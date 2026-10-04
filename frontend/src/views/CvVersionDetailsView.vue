<template>
  <main class="cv-version-detail-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">{{ translate('details.eyebrow') }}</div>
        <h1>{{ snapshot?.name ?? 'Version details' }}<span v-if="version"> · v{{ version.versionNumber }}</span></h1>
        <p>{{ version?.description || 'Saved CV snapshot' }}<span v-if="version"> · {{ formatDate(version.createdAt) }}</span></p>
      </div>
      <div class="content-heading-actions">
        <v-btn variant="text" prepend-icon="mdi-history" :to="`/cvs/${cvId}/versions`">{{ translate('details.allVersions') }}</v-btn>
        <v-btn color="secondary" prepend-icon="mdi-source-branch" :disabled="!version" @click="openBranchDialog">{{ translate('details.branch') }}</v-btn>
        <v-btn color="primary" prepend-icon="mdi-file-pdf-box" :loading="exportingFormat === 'pdf'" :disabled="!version || !!exportingFormat" @click="exportFile('pdf')">{{ translate('details.exportPdf') }}</v-btn>
        <v-btn color="primary" prepend-icon="mdi-file-word-box" :loading="exportingFormat === 'docx'" :disabled="!version || !!exportingFormat" @click="exportFile('docx')">{{ translate('details.exportDocx') }}</v-btn>
        <v-btn color="primary" prepend-icon="mdi-code-json" :loading="exportingFormat === 'json'" :disabled="!version || !!exportingFormat" @click="exportFile('json')">{{ translate('details.exportJson') }}</v-btn>
        <v-btn color="primary" prepend-icon="mdi-backup-restore" @click="restoreDialog = true">{{ translate('details.restore') }}</v-btn>
      </div>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>
    <v-progress-linear v-if="loading" color="primary" indeterminate />

    <template v-else-if="snapshot">
      <div class="snapshot-meta">
        <div><span>{{ translate('details.language') }}</span><strong>{{ snapshot.language }}</strong></div>
        <div><span>{{ translate('details.status') }}</span><strong>{{ translate(`cvForm.${snapshot.status.toLowerCase()}`) }}</strong></div>
        <div><span>{{ translate('details.version') }}</span><strong>v{{ version?.versionNumber }}</strong></div>
      </div>
      <div v-if="version?.parentVersionNumber && version.parentCvId" class="snapshot-lineage">
        {{ translate('details.derivedFrom') }}
        <RouterLink :to="`/cvs/${version.parentCvId}/versions/${version.parentVersionNumber}`">
          {{ version.parentCvId === cvId ? `v${version.parentVersionNumber}` : translate('details.sourceCvVersion', { version: version.parentVersionNumber }) }}
        </RouterLink>
      </div>

      <div class="snapshot-section">
        <h2>{{ translate('details.professionalSummary') }}</h2>
        <p class="snapshot-copy">{{ snapshot.content.summary || translate('details.noSummary') }}</p>
      </div>

      <v-expansion-panels multiple class="snapshot-panels">
        <v-expansion-panel v-if="snapshot.content.experiences.length" :title="translate('details.experience')">
          <v-expansion-panel-text>
            <article v-for="(item, index) in snapshot.content.experiences" :key="index" class="snapshot-entry">
              <h3>{{ item.position }} · {{ item.company }}</h3>
              <p class="muted-cell">{{ item.location }}<span v-if="item.startDate"> · {{ item.startDate }} – {{ item.current ? translate('details.present') : item.endDate }}</span></p>
              <p v-if="item.description" class="snapshot-copy">{{ item.description }}</p>
              <ul v-if="item.projects.length">
                <li v-for="project in item.projects" :key="`${project.projectName}-${project.sortOrder}`">
                  <strong>{{ project.projectName }}</strong><span v-if="project.technologies"> · {{ project.technologies }}</span>
                  <p v-if="project.responsibilities">{{ project.responsibilities }}</p>
                </li>
              </ul>
            </article>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.education.length" :title="translate('details.education')">
          <v-expansion-panel-text>
            <article v-for="(item, index) in snapshot.content.education" :key="index" class="snapshot-entry">
              <h3>{{ item.institution }}</h3>
              <p>{{ [item.degree, item.fieldOfStudy].filter(Boolean).join(' · ') }}</p>
              <p v-if="item.description" class="snapshot-copy">{{ item.description }}</p>
            </article>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.skillGroups.length" :title="translate('details.skills')">
          <v-expansion-panel-text>
            <article v-for="(group, index) in snapshot.content.skillGroups" :key="index" class="snapshot-entry">
              <h3>{{ group.name }}</h3>
              <p>{{ group.skills.map((skill) => skill.level ? `${skill.name} (${skill.level})` : skill.name).join(', ') }}</p>
            </article>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.languages.length" :title="translate('details.languages')">
          <v-expansion-panel-text>
            <p v-for="(item, index) in snapshot.content.languages" :key="index" class="snapshot-line">
              {{ item.language }}<span v-if="item.level"> · {{ item.level }}</span>
            </p>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.projects.length" :title="translate('details.projects')">
          <v-expansion-panel-text>
            <article v-for="(item, index) in snapshot.content.projects" :key="index" class="snapshot-entry">
              <h3>{{ item.name }}<span v-if="item.role"> · {{ item.role }}</span></h3>
              <p v-if="item.description" class="snapshot-copy">{{ item.description }}</p>
              <p v-if="item.technologies" class="muted-cell">{{ item.technologies }}</p>
            </article>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.certifications.length" :title="translate('details.certifications')">
          <v-expansion-panel-text>
            <p v-for="(item, index) in snapshot.content.certifications" :key="index" class="snapshot-line">
              <strong>{{ item.name }}</strong><span v-if="item.issuer"> · {{ item.issuer }}</span>
            </p>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.customSections.length" :title="translate('details.customSections')">
          <v-expansion-panel-text>
            <article v-for="(item, index) in snapshot.content.customSections" :key="index" class="snapshot-entry">
              <h3>{{ item.title }}</h3><p class="snapshot-copy">{{ item.content }}</p>
            </article>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>

    <v-dialog v-model="restoreDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">{{ translate('details.restoreTitle') }}</v-card-title>
        <v-card-text>
          {{ translate('details.restoreDescription') }}
          <v-text-field v-model="restoreDescription" class="mt-4" :label="translate('details.newVersionDescription')" maxlength="500" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="restoreDialog = false">{{ translate('details.cancel') }}</v-btn>
          <v-btn color="primary" :loading="restoring" @click="restore">{{ translate('details.restore') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="branchDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">{{ translate('details.branchTitle') }}</v-card-title>
        <v-card-text>
          {{ translate('details.branchDescription') }}
          <v-text-field v-model="branchName" class="mt-4" :label="translate('details.newCvName')" maxlength="255" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="branchDialog = false">{{ translate('details.cancel') }}</v-btn>
          <v-btn color="primary" :loading="branching" :disabled="!branchName.trim()" @click="createBranch">{{ translate('details.createBranch') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import cvApi from '../shared/api/cvApi'
import type { CvVersionDetail, CvVersionSnapshot } from '../shared/api/cvTypes'
import { locale, translate } from '../shared/i18n'

const route = useRoute()
const router = useRouter()
const cvId = computed(() => typeof route.params.id === 'string' ? route.params.id : '')
const versionNumber = computed(() => Number(route.params.versionNumber))
const version = ref<CvVersionDetail | null>(null)
const snapshot = computed<CvVersionSnapshot | null>(() => version.value?.snapshot ?? null)
const loading = ref(true)
const restoring = ref(false)
const exportingFormat = ref<'pdf' | 'docx' | 'json' | null>(null)
const error = ref('')
const restoreDialog = ref(false)
const restoreDescription = ref('')
const branchDialog = ref(false)
const branchName = ref('')
const branching = ref(false)

watch(() => [route.params.id, route.params.versionNumber], load, { immediate: true })

async function load() {
  loading.value = true
  error.value = ''
  try {
    version.value = await cvApi.getVersion(cvId.value, versionNumber.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('details.loadError')
  } finally {
    loading.value = false
  }
}

async function restore() {
  if (!version.value) return
  restoring.value = true
  error.value = ''
  try {
    const restored = await cvApi.restoreVersion(cvId.value, version.value.versionNumber, restoreDescription.value.trim())
    await router.push(`/cvs/${cvId.value}/versions/${restored.versionNumber}`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('details.restoreError')
  } finally {
    restoring.value = false
    restoreDialog.value = false
  }
}

async function exportFile(format: 'pdf' | 'docx' | 'json') {
  if (!version.value) return
  exportingFormat.value = format
  error.value = ''
  try {
    const file = await cvApi.exportVersion(version.value.id, format)
    const url = URL.createObjectURL(file.content)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(url), 0)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('details.exportError', { format: format.toUpperCase() })
  } finally {
    exportingFormat.value = null
  }
}

function openBranchDialog() {
  branchName.value = `${snapshot.value?.name ?? 'CV'} (Branch)`
  branchDialog.value = true
}

async function createBranch() {
  if (!version.value) return
  branching.value = true
  error.value = ''
  try {
    const branch = await cvApi.branchVersion(cvId.value, version.value.versionNumber, branchName.value.trim())
    await router.push(`/cvs/${branch.id}/content`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('details.branchError')
  } finally {
    branching.value = false
    branchDialog.value = false
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(locale.value === 'lv' ? 'lv-LV' : 'en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
.snapshot-lineage { margin: -10px 0 22px; color: #65756c; font-size: 13px; }
.snapshot-lineage a { color: #205c50; font-weight: 700; }
</style>