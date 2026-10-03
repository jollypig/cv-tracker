<template>
  <main class="cv-version-detail-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">READ-ONLY SNAPSHOT</div>
        <h1>{{ snapshot?.name ?? 'Version details' }}<span v-if="version"> · v{{ version.versionNumber }}</span></h1>
        <p>{{ version?.description || 'Saved CV snapshot' }}<span v-if="version"> · {{ formatDate(version.createdAt) }}</span></p>
      </div>
      <div class="content-heading-actions">
        <v-btn variant="text" prepend-icon="mdi-history" :to="`/cvs/${cvId}/versions`">All versions</v-btn>
        <v-btn color="secondary" prepend-icon="mdi-source-branch" :disabled="!version" @click="openBranchDialog">Branch CV</v-btn>
        <v-btn color="primary" prepend-icon="mdi-file-pdf-box" :loading="exporting" :disabled="!version" @click="exportPdf">Export PDF</v-btn>
        <v-btn color="primary" prepend-icon="mdi-backup-restore" @click="restoreDialog = true">Restore as new version</v-btn>
      </div>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>
    <v-progress-linear v-if="loading" color="primary" indeterminate />

    <template v-else-if="snapshot">
      <div class="snapshot-meta">
        <div><span>LANGUAGE</span><strong>{{ snapshot.language }}</strong></div>
        <div><span>STATUS</span><strong>{{ snapshot.status }}</strong></div>
        <div><span>VERSION</span><strong>v{{ version?.versionNumber }}</strong></div>
      </div>
      <div v-if="version?.parentVersionNumber && version.parentCvId" class="snapshot-lineage">
        Derived from
        <RouterLink :to="`/cvs/${version.parentCvId}/versions/${version.parentVersionNumber}`">
          {{ version.parentCvId === cvId ? `v${version.parentVersionNumber}` : `source CV v${version.parentVersionNumber}` }}
        </RouterLink>
      </div>

      <div class="snapshot-section">
        <h2>Professional summary</h2>
        <p class="snapshot-copy">{{ snapshot.content.summary || 'No summary in this snapshot.' }}</p>
      </div>

      <v-expansion-panels multiple class="snapshot-panels">
        <v-expansion-panel v-if="snapshot.content.experiences.length" title="Experience">
          <v-expansion-panel-text>
            <article v-for="(item, index) in snapshot.content.experiences" :key="index" class="snapshot-entry">
              <h3>{{ item.position }} · {{ item.company }}</h3>
              <p class="muted-cell">{{ item.location }}<span v-if="item.startDate"> · {{ item.startDate }} – {{ item.current ? 'Present' : item.endDate }}</span></p>
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
        <v-expansion-panel v-if="snapshot.content.education.length" title="Education">
          <v-expansion-panel-text>
            <article v-for="(item, index) in snapshot.content.education" :key="index" class="snapshot-entry">
              <h3>{{ item.institution }}</h3>
              <p>{{ [item.degree, item.fieldOfStudy].filter(Boolean).join(' · ') }}</p>
              <p v-if="item.description" class="snapshot-copy">{{ item.description }}</p>
            </article>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.skillGroups.length" title="Skills">
          <v-expansion-panel-text>
            <article v-for="(group, index) in snapshot.content.skillGroups" :key="index" class="snapshot-entry">
              <h3>{{ group.name }}</h3>
              <p>{{ group.skills.map((skill) => skill.level ? `${skill.name} (${skill.level})` : skill.name).join(', ') }}</p>
            </article>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.languages.length" title="Languages">
          <v-expansion-panel-text>
            <p v-for="(item, index) in snapshot.content.languages" :key="index" class="snapshot-line">
              {{ item.language }}<span v-if="item.level"> · {{ item.level }}</span>
            </p>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.projects.length" title="Projects">
          <v-expansion-panel-text>
            <article v-for="(item, index) in snapshot.content.projects" :key="index" class="snapshot-entry">
              <h3>{{ item.name }}<span v-if="item.role"> · {{ item.role }}</span></h3>
              <p v-if="item.description" class="snapshot-copy">{{ item.description }}</p>
              <p v-if="item.technologies" class="muted-cell">{{ item.technologies }}</p>
            </article>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.certifications.length" title="Certifications">
          <v-expansion-panel-text>
            <p v-for="(item, index) in snapshot.content.certifications" :key="index" class="snapshot-line">
              <strong>{{ item.name }}</strong><span v-if="item.issuer"> · {{ item.issuer }}</span>
            </p>
          </v-expansion-panel-text>
        </v-expansion-panel>
        <v-expansion-panel v-if="snapshot.content.customSections.length" title="Custom sections">
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
        <v-card-title class="dialog-title">Restore this snapshot?</v-card-title>
        <v-card-text>
          Restoring creates a new version and leaves the selected historical snapshot unchanged.
          <v-text-field v-model="restoreDescription" class="mt-4" label="New version description" maxlength="500" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="restoreDialog = false">Cancel</v-btn>
          <v-btn color="primary" :loading="restoring" @click="restore">Restore as new version</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="branchDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">Branch from this version</v-card-title>
        <v-card-text>
          The selected snapshot will be copied into a new draft CV and linked to its source version.
          <v-text-field v-model="branchName" class="mt-4" label="New CV name" maxlength="255" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="branchDialog = false">Cancel</v-btn>
          <v-btn color="primary" :loading="branching" :disabled="!branchName.trim()" @click="createBranch">Create branch</v-btn>
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

const route = useRoute()
const router = useRouter()
const cvId = computed(() => typeof route.params.id === 'string' ? route.params.id : '')
const versionNumber = computed(() => Number(route.params.versionNumber))
const version = ref<CvVersionDetail | null>(null)
const snapshot = computed<CvVersionSnapshot | null>(() => version.value?.snapshot ?? null)
const loading = ref(true)
const restoring = ref(false)
const exporting = ref(false)
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
    error.value = cause instanceof Error ? cause.message : 'Unable to load this CV version.'
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
    error.value = cause instanceof Error ? cause.message : 'Unable to restore this CV version.'
  } finally {
    restoring.value = false
    restoreDialog.value = false
  }
}

async function exportPdf() {
  if (!version.value) return
  exporting.value = true
  error.value = ''
  try {
    const file = await cvApi.exportVersionPdf(version.value.id)
    const url = URL.createObjectURL(file.content)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(url), 0)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to export this CV version.'
  } finally {
    exporting.value = false
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
    error.value = cause instanceof Error ? cause.message : 'Unable to branch from this version.'
  } finally {
    branching.value = false
    branchDialog.value = false
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
.snapshot-lineage { margin: -10px 0 22px; color: #65756c; font-size: 13px; }
.snapshot-lineage a { color: #205c50; font-weight: 700; }
</style>