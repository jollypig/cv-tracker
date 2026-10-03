<template>
  <div v-if="skill.details" class="skill-details">
    <div class="skill-details-grid">
      <v-text-field :model-value="skill.details.yearsOfExperience" label="Years of experience" type="number" min="0" step="0.1" variant="outlined" :rules="[nonNegativeRule]" @update:model-value="skill.details.yearsOfExperience = numberOrNull($event)" />
      <v-text-field :model-value="skill.details.yearsActivelyUsed" label="Years actively used" type="number" min="0" step="0.1" variant="outlined" :rules="[nonNegativeRule]" @update:model-value="skill.details.yearsActivelyUsed = numberOrNull($event)" />
      <v-text-field v-model="skill.details.startedFrom" label="Started from" placeholder="YYYY or YYYY-MM-DD" variant="outlined" clearable :rules="[dateRule, dateOrderRule]" />
      <v-text-field v-model="skill.details.lastUsed" label="Last used" placeholder="YYYY or YYYY-MM-DD" variant="outlined" clearable :rules="[dateRule, dateOrderRule]" />
      <v-select v-model="skill.details.frequency" :items="['daily', 'occasionally', 'rarely']" label="Frequency" variant="outlined" clearable />
      <v-select v-model="skill.details.status" :items="['active', 'learning', 'maintaining', 'deprecated']" label="Status" variant="outlined" clearable />
    </div>
    <dl class="skill-metrics">
      <div><dt>Total experience</dt><dd>{{ metrics.totalExperience ?? '-' }} years</dd></div>
      <div><dt>Last used</dt><dd>{{ metrics.lastUsed ?? '-' }}</dd></div>
    </dl>
    <p v-if="metrics.stale" class="skill-warning" role="status"><v-icon icon="mdi-clock-alert-outline" size="small" /> Not used in over 5 years</p>
    <v-autocomplete
      v-model="selectedProjects"
      :items="projectOptions"
      :custom-filter="(_value, query, item) => matchesSkillProject(item?.raw, query)"
      item-title="title"
      item-value="key"
      label="Linked projects"
      variant="outlined"
      density="compact"
      multiple
      chips
      closable-chips
      clearable
      no-data-text="No matching projects"
    >
      <template #item="{ props: itemProps, item }">
        <v-list-item v-bind="itemProps" :subtitle="item.raw.description" />
      </template>
    </v-autocomplete>
    <div v-for="(link, index) in skill.details.linkedProjects" :key="link.projectKey" class="skill-link-row">
      <div class="skill-linked-project">{{ projects.find((project) => project.key === link.projectKey)?.title || 'Project removed' }}</div>
      <v-text-field v-model="link.outcome" label="Outcome" maxlength="1000" variant="outlined" density="compact" />
      <v-btn icon="mdi-link-off" aria-label="Unlink project" variant="text" size="small" @click="skill.details.linkedProjects.splice(index, 1)" />
    </div>
    <v-checkbox v-model="skill.details.includeInOutput" label="Include experience and project outcomes in output" density="compact" hide-details />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CvContent, CvSkill } from '../shared/api/cvTypes'
import { calculateSkill, matchesSkillProject, selectSkillProjects, skillDate, skillProjects } from '../shared/skillMetrics'

const props = defineProps<{ skill: CvSkill; content: CvContent }>()
const projects = computed(() => skillProjects(props.content))
const projectOptions = computed(() => [
  ...projects.value.filter((project) => project.key),
  ...(props.skill.details?.linkedProjects ?? [])
    .filter((link) => !projects.value.some((project) => project.key === link.projectKey))
    .map((link) => ({ key: link.projectKey, title: 'Project removed', description: '' })),
])
const selectedProjects = computed({
  get: () => props.skill.details?.linkedProjects.map((link) => link.projectKey) ?? [],
  set: (keys: string[]) => selectSkillProjects(props.skill, keys ?? []),
})
const metrics = computed(() => calculateSkill(props.skill, props.content))

function numberOrNull(value: string | number | null): number | null {
  return value === null || value === '' ? null : Number(value)
}

function nonNegativeRule(value: string | number | null) {
  return value === null || value === '' || (Number.isFinite(Number(value)) && Number(value) >= 0) || 'Enter a non-negative number.'
}

function dateRule(value: string | null) {
  return !value || Boolean(skillDate(value)) || 'Enter a valid year or date (YYYY-MM-DD).'
}

function dateOrderRule() {
  const start = skillDate(props.skill.details?.startedFrom)
  const end = skillDate(props.skill.details?.lastUsed)
  return !start || !end || start <= end || 'Started from must not be after last used.'
}

</script>

<style scoped>
.skill-details { padding: 12px 0 20px; border-bottom: 1px solid #e8e9e3; margin-bottom: 16px; }
.skill-details-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 12px; }
.skill-metrics { display: flex; flex-wrap: wrap; gap: 12px 24px; margin: 0 0 16px; font-size: 12px; }
.skill-metrics dt { color: #627168; }
.skill-metrics dd { margin: 2px 0 0; font-weight: 600; }
.skill-warning { color: #986017; font-size: 12px; margin: 0 0 16px; }
.skill-link-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 0 8px; }
.skill-linked-project { grid-column: 1 / -1; overflow-wrap: anywhere; font-size: 12px; margin-bottom: 6px; }
@media (max-width: 560px) { .skill-details-grid { grid-template-columns: minmax(0, 1fr); } }
</style>