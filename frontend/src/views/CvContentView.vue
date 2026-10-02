<template>
  <main class="cv-content-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">STRUCTURED CV</div>
        <h1>{{ cv?.name ?? 'Edit CV content' }}</h1>
        <p>{{ cv?.personName ?? 'Build each section of this CV.' }}</p>
      </div>
      <div class="content-heading-actions">
        <v-btn variant="text" prepend-icon="mdi-history" :to="`/cvs/${cvId}/versions`">Versions</v-btn>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/cvs/${cvId}/edit`">CV details</v-btn>
        <v-btn color="primary" :loading="saving" prepend-icon="mdi-content-save-outline" @click="saveContent">Save content</v-btn>
      </div>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>
    <v-alert v-if="saved" class="view-alert" type="success" variant="tonal" closable @click:close="saved = false">
      CV content saved.
    </v-alert>
    <v-progress-linear v-if="loading" class="form-loading" color="primary" indeterminate />

    <div v-if="!loading && cv && person" class="content-workspace">
    <v-form ref="editorForm" class="content-editor" @submit.prevent="saveContent">
      <section class="content-order-section">
        <div class="form-section-heading">
          <h2>Section order and visibility</h2>
          <p>Drag sections into reading order, or change their order with the arrow controls.</p>
        </div>
        <div class="content-section-order">
          <div
            v-for="(section, index) in content.sections"
            :key="section.type"
            class="content-order-row"
            draggable="true"
            @dragstart="draggedSection = index"
            @dragover.prevent
            @drop="dropSection(index)"
            @dragend="draggedSection = null"
          >
            <v-icon icon="mdi-drag" class="drag-handle" aria-hidden="true" />
            <span class="order-number">{{ index + 1 }}</span>
            <span class="order-label">{{ sectionTitle(section.type) }}</span>
            <v-switch v-model="section.visible" :aria-label="`Show ${sectionTitle(section.type)}`" color="primary" density="compact" hide-details />
            <v-btn :disabled="index === 0" aria-label="Move section up" icon="mdi-arrow-up" size="small" variant="text" @click="moveSection(index, -1)" />
            <v-btn :disabled="index === content.sections.length - 1" aria-label="Move section down" icon="mdi-arrow-down" size="small" variant="text" @click="moveSection(index, 1)" />
          </div>
        </div>
      </section>

      <v-expansion-panels multiple class="content-panels">
        <v-expansion-panel value="summary">
          <v-expansion-panel-title>Professional summary</v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-textarea v-model="content.summary" label="Summary" rows="5" counter="10000" variant="outlined" />
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="experience">
          <v-expansion-panel-title>Experience <span class="panel-count">{{ content.experiences.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(experience, index) in content.experiences" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>Position {{ index + 1 }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" aria-label="Move experience up" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.experiences, index, -1)" />
                <v-btn :disabled="index === content.experiences.length - 1" aria-label="Move experience down" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.experiences, index, 1)" />
                <v-btn :aria-label="`Remove position ${index + 1}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.experiences, index)" />
              </div></div>
              <div class="content-field-grid">
                <v-text-field v-model="experience.company" label="Company" variant="outlined" :rules="[requiredRule]" />
                <v-text-field v-model="experience.position" label="Position" variant="outlined" :rules="[requiredRule]" />
                <v-text-field v-model="experience.location" label="Location" variant="outlined" />
                <v-select v-model="experience.employmentType" :items="employmentTypes" label="Employment type" variant="outlined" clearable />
                <v-select v-model="experience.employmentLocation" :items="employmentLocations" label="Employment location" variant="outlined" clearable />
                <v-text-field v-model="experience.startDate" label="Start date" type="date" variant="outlined" />
                <v-text-field v-model="experience.endDate" label="End date" type="date" variant="outlined" :rules="[() => dateOrderRule(experience.startDate, experience.endDate)]" />
                <v-checkbox v-model="experience.current" label="I currently work here" hide-details />
              </div>
              <v-textarea v-model="experience.description" label="Description" rows="3" variant="outlined" />
              <div class="nested-editor">
                <div class="entry-heading"><h4>Projects at this position</h4><v-btn size="small" prepend-icon="mdi-plus" variant="text" @click="addExperienceProject(experience)">Add project</v-btn></div>
                <div v-for="(project, projectIndex) in experience.projects" :key="projectIndex" class="nested-entry">
                  <div class="entry-heading"><span>Project {{ projectIndex + 1 }}</span><v-btn :aria-label="`Remove project ${projectIndex + 1}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(experience.projects, projectIndex)" /></div>
                  <div class="content-field-grid">
                    <v-checkbox
                        :model-value="project.showProjectName === false && project.showCustomerCompany === false"
                        class="project-visibility-toggle"
                        label="Hide Project Name and Company"
                        hide-details
                        @update:model-value="setProjectNameAndCompanyHidden(project, $event)"
                    />
                    <v-text-field v-model="project.projectName" label="Project name" variant="outlined" :rules="[requiredRule]" />
                    <v-text-field v-model="project.company" label="Customer company" variant="outlined" />
                    <v-textarea v-model="project.projectDescription" class="project-description-field" label="Project description" rows="2" variant="outlined" />
                    <v-text-field v-model="project.industries" label="Industries" variant="outlined" />
                    <v-text-field v-model="project.position" label="Project position" variant="outlined" />
                    <v-text-field v-model="project.periodFrom" label="From" type="date" variant="outlined" />
                    <v-text-field v-model="project.periodTo" label="To" type="date" variant="outlined" :rules="[() => dateOrderRule(project.periodFrom, project.periodTo)]" />
                    <v-text-field v-model.number="project.teamSize" label="Team size" type="number" min="0" variant="outlined" />
                    <v-text-field v-model="project.externalLink" label="External link" type="url" variant="outlined" :rules="[optionalUrlRule]" />
                  </div>
                  <v-textarea v-model="project.responsibilities" label="Responsibilities" rows="2" variant="outlined" />
                  <v-textarea v-model="project.technologies" label="Technologies and tools" rows="2" variant="outlined" />
                </div>
              </div>
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addExperience">Add experience</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="education">
          <v-expansion-panel-title>Education <span class="panel-count">{{ content.education.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(education, index) in content.education" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>Education {{ index + 1 }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" aria-label="Move education up" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.education, index, -1)" />
                <v-btn :disabled="index === content.education.length - 1" aria-label="Move education down" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.education, index, 1)" />
                <v-btn :aria-label="`Remove education ${index + 1}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.education, index)" />
              </div></div>
              <div class="content-field-grid">
                <v-text-field v-model="education.institution" label="Institution" variant="outlined" :rules="[requiredRule]" />
                <v-select v-model="education.degree" :items="degreeOptions" label="Degree" clearable variant="outlined" />
                <v-text-field v-model="education.fieldOfStudy" label="Field of study" variant="outlined" />
                <v-text-field v-model="education.startDate" label="Start date" type="date" variant="outlined" />
                <v-text-field v-model="education.endDate" label="End date" type="date" variant="outlined" :rules="[() => dateOrderRule(education.startDate, education.endDate)]" />
                <v-checkbox v-model="education.current" label="I currently study here" hide-details />
              </div>
              <v-textarea v-model="education.diplomaDegreeWork" label="Diploma/Degree Work" rows="2" variant="outlined" />
              <v-textarea v-model="education.description" label="Description" rows="2" variant="outlined" />
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addEducation">Add education</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="skills">
          <v-expansion-panel-title>Skill groups <span class="panel-count">{{ content.skillGroups.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-expansion-panels multiple class="skill-group-panels">
              <v-expansion-panel v-for="(group, groupIndex) in content.skillGroups" :key="groupIndex" :value="groupIndex">
                <v-expansion-panel-title>
                  <span>{{ group.name || `Skill group ${groupIndex + 1}` }}</span>
                  <span class="panel-count">{{ group.skills.length }}</span>
                  <template #actions>
                    <div class="entry-actions" @click.stop>
                      <v-btn :disabled="groupIndex === 0" aria-label="Move skill group up" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.skillGroups, groupIndex, -1)" />
                      <v-btn :disabled="groupIndex === content.skillGroups.length - 1" aria-label="Move skill group down" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.skillGroups, groupIndex, 1)" />
                      <v-btn :aria-label="`Remove ${group.name || 'skill group'}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.skillGroups, groupIndex)" />
                      <v-icon class="skill-group-expand-icon" icon="mdi-chevron-down" aria-hidden="true" />
                    </div>
                  </template>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <v-text-field v-model="group.name" label="Group name" variant="outlined" :rules="[requiredRule]" />
                  <div v-for="(skill, skillIndex) in group.skills" :key="skillIndex" class="content-field-grid skill-entry">
                    <v-text-field v-model="skill.name" label="Skill" variant="outlined" :rules="[requiredRule]" />
                    <v-select v-model="skill.level" :items="skillLevels" label="Level" variant="outlined" />
                    <div class="entry-actions">
                      <v-btn :disabled="skillIndex === 0" aria-label="Move skill up" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(group.skills, skillIndex, -1)" />
                      <v-btn :disabled="skillIndex === group.skills.length - 1" aria-label="Move skill down" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(group.skills, skillIndex, 1)" />
                      <v-btn :aria-label="`Remove skill ${skillIndex + 1}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(group.skills, skillIndex)" />
                    </div>
                  </div>
                  <v-btn size="small" prepend-icon="mdi-plus" variant="text" @click="addSkill(group)">Add skill</v-btn>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addSkillGroup">Add skill group</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="languages">
          <v-expansion-panel-title>Languages <span class="panel-count">{{ content.languages.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(language, index) in content.languages" :key="index" class="language-entry">
              <div class="content-field-grid list-entry">
                <v-text-field v-model="language.language" label="Language" variant="outlined" :rules="[requiredRule]" />
                <v-select v-model="language.level" :items="languageLevels" label="Proficiency" variant="outlined" />
                <div class="entry-actions">
                  <v-btn :disabled="index === 0" aria-label="Move language up" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.languages, index, -1)" />
                  <v-btn :disabled="index === content.languages.length - 1" aria-label="Move language down" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.languages, index, 1)" />
                  <v-btn :aria-label="`Remove language ${index + 1}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.languages, index)" />
                </div>
              </div>
              <v-checkbox :model-value="hasDetailedLevels(language)" density="compact" hide-details label="Add skill-specific levels" @update:model-value="setDetailedLevels(language, $event)" />
              <div v-if="hasDetailedLevels(language)" class="language-detail-grid">
                <v-select v-model="language.reading" :items="languageLevels" label="Reading" variant="outlined" />
                <v-select v-model="language.writing" :items="languageLevels" label="Writing" variant="outlined" />
                <v-select v-model="language.speaking" :items="languageLevels" label="Speaking" variant="outlined" />
              </div>
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addLanguage">Add language</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="projects">
          <v-expansion-panel-title>Projects <span class="panel-count">{{ content.projects.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(project, index) in content.projects" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>{{ project.name || `Project ${index + 1}` }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" aria-label="Move project up" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.projects, index, -1)" />
                <v-btn :disabled="index === content.projects.length - 1" aria-label="Move project down" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.projects, index, 1)" />
                <v-btn :aria-label="`Remove project ${index + 1}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.projects, index)" />
              </div></div>
              <div class="content-field-grid">
                <v-text-field v-model="project.name" label="Project name" variant="outlined" :rules="[requiredRule]" />
                <v-text-field v-model="project.role" label="Role" variant="outlined" />
                <v-text-field v-model="project.technologies" label="Technologies" variant="outlined" />
                <v-text-field v-model="project.url" label="Project URL" type="url" variant="outlined" :rules="[optionalUrlRule]" />
              </div>
              <v-textarea v-model="project.description" label="Description" rows="2" variant="outlined" />
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addProject">Add project</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="certifications">
          <v-expansion-panel-title>Certifications <span class="panel-count">{{ content.certifications.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(certification, index) in content.certifications" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>{{ certification.name || `Certification ${index + 1}` }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" aria-label="Move certification up" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.certifications, index, -1)" />
                <v-btn :disabled="index === content.certifications.length - 1" aria-label="Move certification down" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.certifications, index, 1)" />
                <v-btn :aria-label="`Remove certification ${index + 1}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.certifications, index)" />
              </div></div>
              <div class="content-field-grid">
                <v-text-field v-model="certification.name" label="Certification" variant="outlined" :rules="[requiredRule]" />
                <v-text-field v-model="certification.issuer" label="Issuing organization" variant="outlined" />
                <v-text-field v-model="certification.issueDate" label="Issue date" type="date" variant="outlined" />
                <v-text-field v-model="certification.expiryDate" label="Expiry date" type="date" variant="outlined" :rules="[() => dateOrderRule(certification.issueDate, certification.expiryDate)]" />
                <v-text-field v-model="certification.credentialId" label="Credential ID" variant="outlined" />
                <v-text-field v-model="certification.credentialUrl" label="Credential URL" type="url" variant="outlined" :rules="[optionalUrlRule]" />
              </div>
              <v-textarea v-model="certification.description" label="Description" variant="outlined" rows="3" auto-grow />
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addCertification">Add certification</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="custom">
          <v-expansion-panel-title>Custom sections <span class="panel-count">{{ content.customSections.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(section, index) in content.customSections" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>{{ section.title || `Custom section ${index + 1}` }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" aria-label="Move custom section up" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.customSections, index, -1)" />
                <v-btn :disabled="index === content.customSections.length - 1" aria-label="Move custom section down" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.customSections, index, 1)" />
                <v-btn :aria-label="`Remove custom section ${index + 1}`" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.customSections, index)" />
              </div></div>
              <v-text-field v-model="section.title" label="Section title" variant="outlined" :rules="[requiredRule]" />
              <v-textarea v-model="section.content" label="Content" rows="4" variant="outlined" />
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addCustomSection">Add custom section</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>

      <div class="form-actions content-save-actions">
        <v-btn variant="text" :to="`/cvs/${cvId}/edit`">Cancel</v-btn>
        <v-btn color="primary" type="submit" :loading="saving" prepend-icon="mdi-content-save-outline">Save content</v-btn>
      </div>
    </v-form>
    <CvPreview
      :cv="cv"
      :person="person"
      :content="content"
      :templates="templates"
      :selected-template-id="selectedTemplateId"
      :disabled="selectingTemplate"
      @select-template="selectTemplate"
    />
    </div>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import CvPreview from '../components/templates/CvPreview.vue'
import cvApi from '../shared/api/cvApi'
import personApi from '../shared/api/personApi'
import type {
  Cv, CvCertification, CvContent, CvCustomSection, CvEducation, CvExperience, CvExperienceProject, CvTemplate,
  CvLanguage, CvProject, CvSection, CvSectionType, CvSkill, CvSkillGroup,
} from '../shared/api/cvTypes'
import type { Person } from '../shared/api/personTypes'

const route = useRoute()
const cvId = typeof route.params.id === 'string' ? route.params.id : ''
const cv = ref<Cv | null>(null)
const person = ref<Person | null>(null)
const templates = ref<CvTemplate[]>([])
const selectedTemplateId = ref('')
const loading = ref(true)
const saving = ref(false)
const selectingTemplate = ref(false)
const error = ref('')
const saved = ref(false)
const draggedSection = ref<number | null>(null)
const editorForm = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const sectionDefinitions: Array<{ type: CvSectionType; title: string }> = [
  { type: 'SUMMARY', title: 'Professional summary' },
  { type: 'EXPERIENCE', title: 'Experience' },
  { type: 'EDUCATION', title: 'Education' },
  { type: 'SKILLS', title: 'Skills' },
  { type: 'LANGUAGES', title: 'Languages' },
  { type: 'PROJECTS', title: 'Projects' },
  { type: 'CERTIFICATIONS', title: 'Certifications' },
  { type: 'CUSTOM', title: 'Custom sections' },
]
const languageLevels = ['Native (C2)', 'Fluent (C1)', 'Advanced (B2)', 'Intermediate (B1)', 'Basic (A1–A2)']
const skillLevels = ['Basic', 'Intermediate', 'Advanced', 'Expert']
const employmentTypes = ['Full-time', 'Part-time', 'Contract', 'Freelance', 'Internship', 'Self-employed']
const employmentLocations = ['On-site', 'Hybrid', 'Remote']
const degreeOptions = [
  'High School',
  'Professional',
  'Associate Degree',
  'Bachelor’s Degree',
  'Master’s Degree',
  'Doctorate (PhD)',
]
const detailedLanguages = reactive(new Set<CvLanguage>())
const content = reactive<CvContent>(emptyContent())

onMounted(async () => {
  try {
    const [cvData, contentData, templateData] = await Promise.all([
      cvApi.get(cvId), cvApi.getContent(cvId), cvApi.listTemplates(),
    ])
    const personData = await personApi.get(cvData.personId)
    cv.value = cvData
    person.value = personData
    templates.value = templateData
    selectedTemplateId.value = cvData.templateId ?? ''
    Object.assign(content, contentData)
    content.sections.sort((left, right) => left.sortOrder - right.sortOrder)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to load CV content.'
  } finally {
    loading.value = false
  }
})

function emptyContent(): CvContent {
  return {
    summary: '', experiences: [], education: [], skillGroups: [], languages: [], projects: [],
    certifications: [], customSections: [],
    sections: sectionDefinitions.map((section, sortOrder) => ({ type: section.type, visible: true, sortOrder })),
  }
}

function sectionTitle(type: CvSectionType) {
  return sectionDefinitions.find((section) => section.type === type)?.title ?? type
}

async function selectTemplate(templateId: string) {
  if (!cv.value || !templateId || templateId === selectedTemplateId.value) return
  selectingTemplate.value = true
  error.value = ''
  try {
    cv.value = await cvApi.selectTemplate(cvId, templateId)
    selectedTemplateId.value = templateId
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to select this template.'
  } finally {
    selectingTemplate.value = false
  }
}

function requiredRule(value: string | null) {
  return Boolean(value?.trim()) || 'This field is required.'
}

function dateOrderRule(start: string | null, end: string | null) {
  return !start || !end || end >= start || 'End date must be on or after the start date.'
}

function optionalUrlRule(value: string | null) {
  if (!value?.trim()) return true
  try {
    const protocol = new URL(value).protocol
    return (protocol === 'http:' || protocol === 'https:') || 'Enter a valid HTTP or HTTPS URL.'
  } catch {
    return 'Enter a valid HTTP or HTTPS URL.'
  }
}

function normalizeOrder<T extends { sortOrder: number }>(items: T[]) {
  items.forEach((item, index) => { item.sortOrder = index })
}

function moveItem<T extends { sortOrder: number }>(items: T[], index: number, direction: number) {
  const target = index + direction
  if (target < 0 || target >= items.length) return
  ;[items[index], items[target]] = [items[target]!, items[index]!]
  normalizeOrder(items)
}

function removeItem<T extends { sortOrder: number }>(items: T[], index: number) {
  items.splice(index, 1)
  normalizeOrder(items)
}

function moveSection(index: number, direction: number) {
  moveItem(content.sections, index, direction)
}

function dropSection(target: number) {
  const source = draggedSection.value
  if (source === null || source === target) return
  const [section] = content.sections.splice(source, 1)
  if (section) content.sections.splice(target, 0, section)
  normalizeOrder(content.sections)
  draggedSection.value = null
}

function addExperience() {
  content.experiences.push({ company: '', position: '', location: '', employmentType: null, employmentLocation: null, startDate: null, endDate: null,
    current: false, description: '', sortOrder: content.experiences.length, projects: [] })
}

function addExperienceProject(experience: CvExperience) {
  experience.projects.push({ company: '', industries: '', projectName: '', projectDescription: '', showProjectName: true, showCustomerCompany: true,
    periodFrom: null, periodTo: null, position: '', responsibilities: '', technologies: '',
    teamSize: null, externalLink: '', sortOrder: experience.projects.length })
}

function setProjectNameAndCompanyHidden(project: CvExperienceProject, hidden: boolean | null) {
  project.showProjectName = !hidden
  project.showCustomerCompany = !hidden
}

function addEducation() {
  content.education.push({ institution: '', degree: '', diplomaDegreeWork: '', fieldOfStudy: '', startDate: null, endDate: null, current: false,
    description: '', sortOrder: content.education.length })
}

function addSkillGroup() {
  content.skillGroups.push({ name: '', sortOrder: content.skillGroups.length, skills: [] })
}

function addSkill(group: CvSkillGroup) {
  group.skills.push({ name: '', level: '', sortOrder: group.skills.length })
}

function addLanguage() {
  content.languages.push({ language: '', level: '', reading: '', writing: '', speaking: '', sortOrder: content.languages.length })
}

function hasDetailedLevels(language: CvLanguage) {
  return detailedLanguages.has(language) || [language.reading, language.writing, language.speaking]
    .some((level) => Boolean(level?.trim()))
}

function setDetailedLevels(language: CvLanguage, enabled: boolean | null) {
  if (enabled) {
    detailedLanguages.add(language)
    return
  }
  detailedLanguages.delete(language)
  language.reading = ''
  language.writing = ''
  language.speaking = ''
}

function addProject() {
  content.projects.push({ name: '', role: '', description: '', technologies: '', url: '', sortOrder: content.projects.length })
}

function addCertification() {
  content.certifications.push({ name: '', description: '', issuer: '', issueDate: null, expiryDate: null,
    credentialId: '', credentialUrl: '', sortOrder: content.certifications.length })
}

function addCustomSection() {
  content.customSections.push({ title: '', content: '', sortOrder: content.customSections.length })
}

async function saveContent() {
  error.value = ''
  saved.value = false
  const validation = await editorForm.value?.validate()
  if (validation && !validation.valid) return
  saving.value = true
  try {
    content.sections.forEach((section, index) => { section.sortOrder = index })
    const savedContent = await cvApi.saveContent(cvId, content)
    Object.assign(content, savedContent)
    saved.value = true
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to save CV content.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cv-content-view { max-width: 1440px; margin: 0 auto; }
.content-workspace { display: grid; grid-template-columns: minmax(0, 1fr) minmax(360px, 42%); align-items: start; gap: 30px; }
.content-editor { min-width: 0; }
.content-heading-actions { display: flex; align-items: center; gap: 8px; }
.content-order-section { padding: 24px 0; border-bottom: 1px solid #e4e5de; }
.content-section-order { display: grid; max-width: 660px; gap: 5px; }
.content-order-row { display: grid; min-height: 48px; grid-template-columns: 24px 28px minmax(0, 1fr) 76px 36px 36px; align-items: center; gap: 6px; padding: 4px 8px; border: 1px solid #e4e5de; border-radius: 6px; background: #fff; }
.content-order-row[draggable="true"] { cursor: grab; }
.content-order-row:active { cursor: grabbing; }
.drag-handle { color: #8b9891; }
.order-number { color: #8b9891; font-size: 12px; }
.order-label { min-width: 0; color: #28352f; font-size: 13px; font-weight: 600; }
.content-panels { margin-top: 20px; border: 1px solid #e4e5de; border-radius: 6px; background: #fff; }
.content-panels :deep(.v-expansion-panel) { border-bottom: 1px solid #e8e9e3; }
.content-panels :deep(.v-expansion-panel-title) { min-height: 56px; font-weight: 700; }
.skill-group-panels { margin: 0 0 12px; border: 1px solid #e8e9e3; border-radius: 4px; }
.skill-group-panels :deep(.v-expansion-panel-title) { min-height: 48px; font-size: 13px; }
.skill-group-panels :deep(.v-expansion-panel-text__wrapper) { padding: 16px 16px 8px; }
.skill-group-expand-icon { margin-left: 4px; transition: transform 180ms ease; }
.skill-group-panels :deep(.v-expansion-panel-title--active .skill-group-expand-icon) { transform: rotate(180deg); }
.panel-count { margin-left: 8px; color: #78847e; font-size: 12px; font-weight: 500; }
.editor-entry { padding: 17px 0 12px; border-bottom: 1px solid #e8e9e3; }
.editor-entry:first-child { padding-top: 0; }
.entry-heading { display: flex; min-height: 36px; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.entry-heading h3, .entry-heading h4 { margin: 0; color: #28352f; font: 700 14px/1.4 'Manrope', sans-serif; }
.entry-heading h4 { font-size: 13px; }
.entry-actions { display: flex; align-items: center; justify-content: flex-end; flex: 0 0 auto; }
.content-field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 14px; }
.project-description-field { grid-column: 1 / -1; }
.project-visibility-toggle { grid-column: 1 / -1; }
.language-entry { padding-bottom: 12px; }
.language-detail-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0 14px; padding-left: 12px; }
.nested-editor { margin: 14px 0 4px; padding: 14px; border-left: 3px solid #d9e6dc; background: #f8faf8; }
.nested-entry { padding: 12px 0; border-top: 1px solid #e4e5de; }
.nested-entry > .entry-heading { color: #52635a; font-size: 12px; }
.skill-entry, .list-entry { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto; align-items: start; }
.content-save-actions { padding: 24px 0; }
@media (max-width: 680px) {
  .content-heading-actions { align-items: flex-start; flex-direction: column-reverse; }
  .content-order-row { grid-template-columns: 20px 22px minmax(0, 1fr) 66px 32px 32px; gap: 2px; padding: 4px; }
  .content-field-grid, .skill-entry, .list-entry { grid-template-columns: minmax(0, 1fr); gap: 0; }
  .language-detail-grid { grid-template-columns: minmax(0, 1fr); gap: 0; padding-left: 0; }
  .skill-entry .entry-actions, .list-entry .entry-actions { justify-content: flex-end; margin: -10px 0 8px; }
  .nested-editor { padding: 10px; }
}
@media (max-width: 980px) {
  .content-workspace { grid-template-columns: minmax(0, 1fr); gap: 26px; }
  .content-workspace > .preview-panel { grid-row: 1; }
}
</style>
