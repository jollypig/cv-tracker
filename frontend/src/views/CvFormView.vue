<template>
  <main class="cv-form-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">CV DETAILS</div>
        <h1>{{ isEdit ? 'Edit CV' : 'Create a CV' }}</h1>
        <p>{{ isEdit ? 'Update the title, language, or status.' : 'Start a new CV for a person in your workspace.' }}</p>
      </div>
      <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="backRoute">Back to CVs</v-btn>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal">{{ error }}</v-alert>
    <v-progress-linear v-if="loading" class="form-loading" color="primary" indeterminate />

    <form v-else @submit.prevent="saveCv">
      <section class="form-section">
        <div class="form-section-heading">
          <h2>About this CV</h2>
          <p>Choose a clear title so it is easy to find in the library.</p>
        </div>

        <v-select
          v-if="!isEdit && !routePersonId"
          v-model="selectedPersonId"
          :items="personOptions"
          item-title="name"
          item-value="id"
          label="Person"
          variant="outlined"
          :rules="[requiredRule]"
          required
        />
        <v-text-field v-else :model-value="personName" label="Person" variant="outlined" readonly />

        <v-text-field
          v-model="form.name"
          label="CV title"
          placeholder="e.g. Product designer"
          variant="outlined"
          :rules="[requiredRule]"
          maxlength="255"
          required
        />
        <v-textarea
          v-model="form.description"
          label="Description"
          placeholder="Optional notes about this version or its intended role"
          variant="outlined"
          rows="3"
          auto-grow
        />
      </section>

      <section class="form-section">
        <div class="form-section-heading">
          <h2>Language and status</h2>
          <p>Set the language and lifecycle state for this CV.</p>
        </div>
        <div class="cv-form-grid">
          <v-text-field
            v-model="form.language"
            label="Language code"
            placeholder="en"
            variant="outlined"
            :rules="[requiredRule]"
            maxlength="10"
            required
          />
          <v-select v-model="form.status" :items="statuses" label="Status" variant="outlined" />
        </div>
      </section>

      <div class="form-actions">
        <v-btn variant="text" :to="backRoute">Cancel</v-btn>
        <v-btn color="primary" type="submit" :loading="saving" prepend-icon="mdi-content-save-outline">
          {{ isEdit ? 'Save changes' : 'Create CV' }}
        </v-btn>
      </div>
    </form>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCvStore } from '../shared/stores/cvStore'
import { usePersonStore } from '../shared/stores/personStore'
import type { CvInput, CvStatus } from '../shared/api/cvTypes'

const route = useRoute()
const router = useRouter()
const cvStore = useCvStore()
const personStore = usePersonStore()
const routePersonId = typeof route.params.personId === 'string' ? route.params.personId : undefined
const cvId = typeof route.params.id === 'string' ? route.params.id : undefined
const isEdit = computed(() => Boolean(cvId))
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const selectedPersonId = ref(routePersonId ?? '')
const personName = ref('')
const statuses: CvStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED']
const personOptions = computed(() => personStore.people.map((person) => ({
  id: person.id,
  name: `${person.firstName} ${person.lastName}`,
})))
const backRoute = computed(() => routePersonId ? `/people/${routePersonId}/cvs` : '/cvs')
const form = reactive<CvInput>({ name: '', description: null, language: 'en', status: 'DRAFT' })
const requiredRule = (value: string) => Boolean(value?.trim()) || 'This field is required.'

onMounted(async () => {
  try {
    await personStore.fetchPeople()
    if (routePersonId) {
      const person = await personStore.fetchPerson(routePersonId)
      personName.value = `${person.firstName} ${person.lastName}`
    } else if (!cvId && personOptions.value.length) {
      selectedPersonId.value = personOptions.value[0]?.id ?? ''
    }

    if (cvId) {
      const cv = await cvStore.fetchCv(cvId)
      form.name = cv.name
      form.description = cv.description
      form.language = cv.language
      form.status = cv.status
      selectedPersonId.value = cv.personId
      personName.value = cv.personName
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to load this CV.'
  } finally {
    loading.value = false
  }
})

async function saveCv() {
  error.value = ''
  if (!form.name.trim() || !form.language.trim()) {
    error.value = 'Enter a CV title and language.'
    return
  }
  if (!isEdit.value && !selectedPersonId.value) {
    error.value = 'Choose a person for this CV.'
    return
  }

  saving.value = true
  const input: CvInput = {
    ...form,
    name: form.name.trim(),
    description: form.description?.trim() || null,
    language: form.language.trim(),
  }
  try {
    if (cvId) {
      await cvStore.updateCv(cvId, input)
    } else {
      await cvStore.createCv(selectedPersonId.value, input)
    }
    await router.push(backRoute.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to save this CV.'
  } finally {
    saving.value = false
  }
}
</script>