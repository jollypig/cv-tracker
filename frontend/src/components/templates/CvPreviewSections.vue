<template>
  <div class="preview-sections" :class="`preview-sections--${variant}`">
    <template v-for="section in orderedSections" :key="section.type">
      <section v-if="section.visible && section.type === 'SUMMARY' && content.summary?.trim()" class="preview-section">
        <h3>Profile</h3>
        <p>{{ content.summary }}</p>
      </section>

      <section v-else-if="section.visible && section.type === 'EXPERIENCE' && content.experiences.length" class="preview-section">
        <h3>Experience</h3>
        <article v-for="(experience, index) in content.experiences" v-show="hasExperience(experience)" :key="index" class="preview-entry">
          <div class="preview-entry-heading">
            <h4>{{ experience.position || experience.company || 'Position' }}</h4>
            <span>{{ period(experience.startDate, experience.endDate, experience.current) }}</span>
          </div>
          <p class="preview-meta">{{ [experience.company, experience.location, experience.employmentType, experience.employmentLocation].filter(Boolean).join(' · ') }}</p>
          <p v-if="experience.description">{{ experience.description }}</p>
          <div v-for="(project, projectIndex) in experience.projects" :key="projectIndex" class="preview-nested-entry">
            <h5 v-if="project.showProjectName !== false">{{ project.projectName }}</h5>
            <p v-if="project.position || (project.showCustomerCompany !== false && project.company)" class="preview-meta">{{ [project.position, project.showCustomerCompany !== false ? project.company : null].filter(Boolean).join(' · ') }}</p>
            <p v-if="project.projectDescription">{{ project.projectDescription }}</p>
            <p v-if="project.responsibilities">{{ project.responsibilities }}</p>
            <p v-if="project.technologies" class="preview-meta">{{ project.technologies }}</p>
            <p v-if="linkedSkillNames(project).length" class="preview-meta">Skills: {{ linkedSkillNames(project).join(', ') }}</p>
          </div>
        </article>
      </section>

      <section v-else-if="section.visible && section.type === 'EDUCATION' && content.education.length" class="preview-section">
        <h3>Education</h3>
        <article v-for="(education, index) in content.education" v-show="education.institution || education.degree || education.description" :key="index" class="preview-entry">
          <div class="preview-entry-heading">
            <h4>{{ education.degree || education.institution }}</h4>
            <span>{{ period(education.startDate, education.endDate, education.current) }}</span>
          </div>
          <p class="preview-meta">{{ [education.institution, education.fieldOfStudy].filter(Boolean).join(' · ') }}</p>
          <p v-if="education.diplomaDegreeWork">{{ education.diplomaDegreeWork }}</p>
          <p v-if="education.description">{{ education.description }}</p>
        </article>
      </section>

      <section v-else-if="section.visible && section.type === 'SKILLS' && printableSkillGroups.length" class="preview-section">
        <h3>Skills</h3>
        <div v-for="(group, index) in printableSkillGroups" :key="index" class="preview-entry">
          <h4 v-if="group.name">{{ group.name }}</h4>
          <p class="preview-skill-list">{{ group.skills.map((skill) => skill.level ? `${skill.name} · ${skill.level}` : skill.name).join('  |  ') }}</p>
          <template v-for="(skill, skillIndex) in group.skills" :key="skillIndex">
            <p v-for="(line, lineIndex) in skillOutput(skill, content)" :key="lineIndex" class="preview-meta">{{ line }}</p>
          </template>
        </div>
      </section>

      <section v-else-if="section.visible && section.type === 'LANGUAGES' && content.languages.some((language) => language.language)" class="preview-section">
        <h3>Languages</h3>
        <p>{{ content.languages.filter((language) => language.language).map(languageSummary).join('  |  ') }}</p>
      </section>

      <section v-else-if="section.visible && section.type === 'PROJECTS' && content.projects.length" class="preview-section">
        <h3>Projects</h3>
        <article v-for="(project, index) in content.projects" v-show="project.name || project.description || project.technologies" :key="index" class="preview-entry">
          <div class="preview-entry-heading">
            <h4>{{ project.name }}</h4>
            <span>{{ project.role }}</span>
          </div>
          <p v-if="project.description">{{ project.description }}</p>
          <p v-if="project.technologies" class="preview-meta">{{ project.technologies }}</p>
          <a v-if="safeUrl(project.url)" :href="safeUrl(project.url)" target="_blank" rel="noreferrer">{{ project.url }}</a>
        </article>
      </section>

      <section v-else-if="section.visible && section.type === 'CERTIFICATIONS' && content.certifications.length" class="preview-section">
        <h3>Certifications</h3>
        <article v-for="(certification, index) in content.certifications" v-show="certification.name || certification.issuer" :key="index" class="preview-entry">
          <div class="preview-entry-heading">
            <h4>{{ certification.name }}</h4>
            <span>{{ period(certification.issueDate, certification.expiryDate) }}</span>
          </div>
          <p class="preview-meta">{{ [certification.issuer, certification.credentialId].filter(Boolean).join(' · ') }}</p>
          <p v-if="certification.description">{{ certification.description }}</p>
          <a v-if="safeUrl(certification.credentialUrl)" :href="safeUrl(certification.credentialUrl)" target="_blank" rel="noreferrer">Credential</a>
        </article>
      </section>

      <section v-else-if="section.visible && section.type === 'CUSTOM' && content.customSections.length" class="preview-section">
        <template v-for="(custom, index) in content.customSections" :key="index">
          <article v-if="custom.title || custom.content" class="preview-entry">
            <h3>{{ custom.title }}</h3>
            <p>{{ custom.content }}</p>
          </article>
        </template>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CvContent, CvExperience, CvExperienceProject, CvLanguage } from '../../shared/api/cvTypes'
import { skillOutput } from '../../shared/skillMetrics'

const props = defineProps<{
  content: CvContent
  variant: 'modern' | 'classic' | 'minimal'
}>()

const orderedSections = computed(() => [...props.content.sections].sort((left, right) => left.sortOrder - right.sortOrder))
const printableSkillGroups = computed(() => props.content.skillGroups
  .map((group) => ({ ...group, skills: group.skills.filter((skill) => skill.visible !== false && skill.name.trim()) }))
  .filter((group) => group.skills.length > 0))

function hasExperience(experience: CvExperience) {
  return Boolean(experience.position || experience.company || experience.description || experience.projects.length)
}

function linkedSkillNames(project: CvExperienceProject) {
  if (!project.projectKey) return []
  return props.content.skillGroups.flatMap((group) => group.skills
    .filter((skill) => skill.visible !== false && skill.name.trim()
      && skill.details?.linkedProjects.some((link) => link.projectKey === project.projectKey))
    .map((skill) => skill.name))
}

function languageSummary(language: CvLanguage) {
  const details = [
    language.reading && `Reading: ${language.reading}`,
    language.writing && `Writing: ${language.writing}`,
    language.speaking && `Speaking: ${language.speaking}`,
  ].filter(Boolean)
  return [language.language, language.level, details.length ? details.join(', ') : ''].filter(Boolean).join(' · ')
}

function formatDate(value: string | null) {
  if (!value) return ''
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { month: 'short', year: 'numeric' }).format(date)
}

function period(start: string | null, end: string | null, current = false) {
  return [formatDate(start), current ? 'Present' : formatDate(end)].filter(Boolean).join(' - ')
}

function safeUrl(value: string | null) {
  if (!value) return ''
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : ''
  } catch {
    return ''
  }
}
</script>

<style scoped>
.preview-sections { display: grid; gap: 18px; min-width: 0; }
.preview-section { min-width: 0; }
.preview-section > h3 { margin: 0 0 8px; padding-bottom: 5px; border-bottom: 1px solid #dce4dd; color: #315d4b; font-size: 12px; font-weight: 800; text-transform: uppercase; }
.preview-entry { margin: 0 0 11px; break-inside: avoid; }
.preview-entry-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.preview-entry-heading h4, .preview-entry h4 { margin: 0; color: #24352c; font-size: 12px; font-weight: 700; }
.preview-entry-heading > span { flex: 0 0 auto; color: #67766c; font-size: 10px; }
.preview-entry h5 { margin: 6px 0 2px; font-size: 11px; }
.preview-entry p, .preview-section > p { margin: 4px 0 0; color: #35443b; font-size: 11px; line-height: 1.55; white-space: pre-wrap; overflow-wrap: anywhere; }
.preview-entry .preview-meta { color: #66756b; font-size: 10px; }
.preview-nested-entry { margin: 8px 0 0 10px; padding-left: 9px; border-left: 2px solid #dce6df; }
.preview-skill-list { display: flex; flex-wrap: wrap; gap: 4px 10px; }
.preview-entry a { color: #276d5a; font-size: 10px; overflow-wrap: anywhere; }
.preview-sections--classic { gap: 15px; }
.preview-sections--classic .preview-section > h3 { color: #34383a; border-bottom-color: #777; }
.preview-sections--minimal { gap: 13px; }
.preview-sections--minimal .preview-section > h3 { border: 0; padding: 0; color: #465d50; }
</style>