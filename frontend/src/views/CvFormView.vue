<template>
  <main class="cv-form-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">{{ translate('cvForm.eyebrow') }}</div>
        <h1>{{ translate(isEdit ? 'navigation.editCv' : 'cvForm.createTitle') }}</h1>
        <p>{{ translate(isEdit ? 'cvForm.editDescription' : 'cvForm.createDescription') }}</p>
      </div>
      <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="backRoute">{{ translate('cvForm.back') }}</v-btn>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal">{{ error }}</v-alert>
    <v-progress-linear v-if="loading" class="form-loading" color="primary" indeterminate />

    <form v-else @submit.prevent="saveCv">
      <section class="form-section">
        <div class="form-section-heading">
          <h2>{{ translate('cvForm.about') }}</h2>
          <p>{{ translate('cvForm.aboutDescription') }}</p>
        </div>

        <v-select
          v-if="!isEdit && !routePersonId"
          v-model="selectedPersonId"
          :items="personOptions"
          item-title="name"
          item-value="id"
          :label="translate('cvForm.person')"
          variant="outlined"
          :rules="[requiredRule]"
          required
        />
        <v-text-field v-else :model-value="personName" :label="translate('cvForm.person')" variant="outlined" readonly />

        <v-text-field
          v-model="form.name"
          :label="translate('cvForm.title')"
          :placeholder="translate('cvForm.titlePlaceholder')"
          variant="outlined"
          :rules="[requiredRule]"
          maxlength="255"
          required
        />
        <v-textarea
          v-model="form.description"
          :label="translate('cvForm.description')"
          :placeholder="translate('cvForm.descriptionPlaceholder')"
          variant="outlined"
          rows="3"
          auto-grow
        />
        <v-combobox
          v-model="form.tags"
          :label="translate('cvForm.tags')"
          :placeholder="translate('cvForm.tagsPlaceholder')"
          :hint="translate('cvForm.tagsHint')"
          persistent-hint
          variant="outlined"
          multiple
          chips
          closable-chips
          clearable
          :rules="[tagCountRule, tagLengthRule]"
        />
      </section>

      <section class="form-section">
        <div class="form-section-heading">
          <h2>{{ translate('cvForm.languageStatus') }}</h2>
          <p>{{ translate('cvForm.languageStatusDescription') }}</p>
        </div>
        <div class="cv-form-grid">
          <v-text-field
            v-model="form.language"
            :label="translate('cvForm.languageCode')"
            placeholder="en"
            variant="outlined"
            :rules="[requiredRule]"
            maxlength="10"
            required
          />
          <v-select v-model="form.status" :items="statuses" item-title="title" item-value="value" :label="translate('cvForm.status')" variant="outlined" />
        </div>
      </section>

      <div class="form-actions">
        <v-btn variant="text" :to="backRoute">{{ translate('cvForm.cancel') }}</v-btn>
        <v-btn color="primary" type="submit" :loading="saving" prepend-icon="mdi-content-save-outline">
          {{ translate(isEdit ? 'cvForm.save' : 'cvForm.create') }}
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
import { translate } from '../shared/i18n'

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
const statuses = computed(() => (['DRAFT', 'ACTIVE', 'ARCHIVED'] as CvStatus[]).map((value) => ({
  value,
  title: translate(`cvForm.${value.toLowerCase()}`),
})))
const personOptions = computed(() => personStore.people.map((person) => ({
  id: person.id,
  name: `${person.firstName} ${person.lastName}`,
})))
const backRoute = computed(() => routePersonId ? `/people/${routePersonId}/cvs` : '/cvs')
const form = reactive<CvInput>({ name: '', description: null, language: 'en', status: 'DRAFT', tags: [] })
const requiredRule = (value: string) => Boolean(value?.trim()) || translate('cvForm.required')
const tagCountRule = (values: string[]) => values.length <= 20 || translate('cvForm.tagCount')
const tagLengthRule = (values: string[]) => values.every((value) => value.trim().length <= 50) || translate('cvForm.tagLength')

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
      form.tags = [...cv.tags]
      selectedPersonId.value = cv.personId
      personName.value = cv.personName
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('cvForm.loadError')
  } finally {
    loading.value = false
  }
})

async function saveCv() {
  error.value = ''
  if (!form.name.trim() || !form.language.trim()) {
    error.value = translate('cvForm.titleLanguageRequired')
    return
  }
  if (!isEdit.value && !selectedPersonId.value) {
    error.value = translate('cvForm.choosePerson')
    return
  }

  saving.value = true
  const input: CvInput = {
    ...form,
    name: form.name.trim(),
    description: form.description?.trim() || null,
    language: form.language.trim(),
    tags: form.tags.map((tag) => tag.trim()).filter(Boolean),
  }
  try {
    if (cvId) {
      await cvStore.updateCv(cvId, input)
    } else {
      await cvStore.createCv(selectedPersonId.value, input)
    }
    await router.push(backRoute.value)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('cvForm.saveError')
  } finally {
    saving.value = false
  }
}
</script>