<template>
  <main class="person-form-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">{{ translate('personForm.eyebrow') }}</div>
        <h1>{{ translate(isEditing ? 'personForm.editTitle' : 'personForm.addTitle') }}</h1>
        <p>{{ translate(isEditing ? 'personForm.editDescription' : 'personForm.addDescription') }}</p>
      </div>
      <v-btn icon="mdi-arrow-left" :aria-label="translate('personForm.back')" to="/" variant="text" />
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal">
      {{ error }}
    </v-alert>
    <v-progress-linear v-if="loadingPerson" class="form-loading" color="primary" indeterminate />

    <v-form v-else @submit.prevent="savePerson">
      <section class="form-section" aria-labelledby="profile-section-title">
        <div class="form-section-heading">
          <h2 id="profile-section-title">{{ translate('personForm.profile') }}</h2>
          <p>{{ translate('personForm.requiredHint') }}</p>
        </div>
        <div class="person-photo-field">
          <v-avatar size="112" color="surface-variant">
            <v-img v-if="photoPreview" :src="photoPreview" :alt="translate('personForm.photo')" cover />
            <v-icon v-else icon="mdi-account" size="48" />
          </v-avatar>
          <div class="person-photo-actions">
            <input ref="photoInput" class="visually-hidden" type="file" accept="image/jpeg,image/png" @change="selectPhoto">
            <v-btn prepend-icon="mdi-camera-outline" variant="tonal" @click="photoInput?.click()">{{ translate('personForm.choosePhoto') }}</v-btn>
            <span class="field-hint">{{ translate('personForm.photoHint') }}</span>
          </div>
        </div>
        <v-row>
          <v-col cols="12" sm="6">
            <v-text-field
              v-model="form.firstName"
              :rules="[required, maxLength(100)]"
              :label="translate('personForm.firstName')"
              maxlength="100"
              required
            />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field
              v-model="form.lastName"
              :rules="[required, maxLength(100)]"
              :label="translate('personForm.lastName')"
              maxlength="100"
              required
            />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field v-model="form.position" :label="translate('personForm.position')" maxlength="255" />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field v-model="form.dateOfBirth" :label="translate('personForm.birthDate')" type="date" />
          </v-col>
          <v-col cols="12" sm="6">
            <v-select v-model="form.gender" :items="genderOptions" item-title="title" item-value="value" :label="translate('personForm.gender')" clearable />
          </v-col>
          <v-col cols="12" sm="6">
            <v-select v-model="form.maritalStatus" :items="maritalStatusOptions" item-title="title" item-value="value" :label="translate('personForm.maritalStatus')" clearable />
          </v-col>
          <v-col cols="12" sm="6">
            <v-select v-model="form.militaryStatus" :items="militaryStatusOptions" item-title="title" item-value="value" :label="translate('personForm.militaryStatus')" clearable />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field v-model="form.location" :label="translate('personForm.location')" maxlength="255" />
          </v-col>
        </v-row>
      </section>

      <section class="form-section contacts-section" aria-labelledby="contacts-section-title">
        <div class="form-section-heading contacts-heading">
          <div>
            <h2 id="contacts-section-title">{{ translate('personForm.contacts') }}</h2>
            <p>{{ translate('personForm.contactDescription') }}</p>
          </div>
          <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addContact">{{ translate('personForm.addContact') }}</v-btn>
        </div>
        <div v-if="contacts.length === 0" class="contacts-empty">{{ translate('personForm.noContacts') }}</div>
        <div v-for="(contact, index) in contacts" :key="contact.key" class="contact-row">
          <v-select
            v-model="contact.type"
            :items="contactTypes"
            :label="translate('personForm.type')"
            item-title="title"
            item-value="value"
            class="contact-type-field"
          />
          <v-text-field
            v-model="contact.value"
            :rules="[required, maxLength(500)]"
            :label="translate('personForm.value')"
            maxlength="500"
            required
          />
          <v-checkbox v-model="contact.primary" hide-details :label="translate('personForm.primary')" />
          <v-checkbox v-model="contact.showContact" hide-details :label="translate('personForm.showContactInCv')" />
          <v-tooltip :text="translate('personForm.removeContact')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('personForm.removeContactNumber', { number: index + 1 })" class="remove-contact" color="error" icon="mdi-close" variant="text" @click="removeContact(index)" />
            </template>
          </v-tooltip>
        </div>
      </section>

      <div class="form-actions">
        <v-btn variant="text" to="/">{{ translate('personForm.cancel') }}</v-btn>
        <v-btn color="primary" :loading="saving" type="submit">
          {{ translate(isEditing ? 'personForm.save' : 'personForm.create') }}
        </v-btn>
      </div>
    </v-form>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePersonStore } from '../shared/stores/personStore'
import personApi from '../shared/api/personApi'
import type { ContactType, PersonContactInput } from '../shared/api/personTypes'
import { translate } from '../shared/i18n'

const route = useRoute()
const router = useRouter()
const store = usePersonStore()
const isEditing = computed(() => typeof route.params.id === 'string')
const loadingPerson = ref(false)
const saving = ref(false)
const error = ref('')
const photoInput = ref<HTMLInputElement | null>(null)
const selectedPhoto = ref<File | null>(null)
const photoPreview = ref('')
const photoStorageKey = ref<string | null>(null)
let photoObjectUrl: string | null = null
const form = reactive({
  firstName: '', lastName: '', dateOfBirth: '', position: '',
  gender: '', maritalStatus: '', militaryStatus: '', location: '',
})
const contacts = ref<Array<PersonContactInput & { key: number }>>([])
let nextContactKey = 0

const genderOptions = computed(() => [
  { title: translate('personForm.male'), value: 'Male' },
  { title: translate('personForm.female'), value: 'Female' },
])
const maritalStatusOptions = computed(() => [
  { title: translate('personForm.single'), value: 'Single' },
  { title: translate('personForm.married'), value: 'Married' },
])
const militaryStatusOptions = computed(() => [
  { title: translate('personForm.completed'), value: 'Completed' },
  { title: translate('personForm.notCompleted'), value: 'Not Completed' },
  { title: translate('personForm.exempt'), value: 'Exempt' },
  { title: translate('personForm.currentlyServing'), value: 'Currently Serving' },
  { title: translate('personForm.notApplicable'), value: 'Not Applicable' },
])

const contactTypes = computed<Array<{ title: string; value: ContactType }>>(() => [
  { title: translate('contact.email'), value: 'EMAIL' },
  { title: translate('contact.phone'), value: 'PHONE' },
  { title: translate('contact.linkedin'), value: 'LINKEDIN' },
  { title: translate('contact.github'), value: 'GITHUB' },
  { title: translate('contact.website'), value: 'WEBSITE' },
  { title: translate('contact.facebook'), value: 'FACEBOOK' },
  { title: translate('contact.whatsapp'), value: 'WHATSAPP' },
  { title: translate('contact.viber'), value: 'VIBER' },
  { title: translate('contact.telegram'), value: 'TELEGRAM' },
  { title: translate('contact.instagram'), value: 'INSTAGRAM' },
  { title: translate('contact.address'), value: 'ADDRESS' },
  { title: translate('contact.other'), value: 'OTHER' },
])

onMounted(async () => {
  if (!isEditing.value) return
  loadingPerson.value = true
  try {
    const person = await store.fetchPerson(String(route.params.id))
    form.firstName = person.firstName
    form.lastName = person.lastName
    form.dateOfBirth = person.dateOfBirth ?? ''
    form.position = person.position ?? ''
    form.gender = person.gender ?? ''
    form.maritalStatus = person.maritalStatus ?? ''
    form.militaryStatus = person.militaryStatus ?? ''
    form.location = person.location ?? ''
    photoStorageKey.value = person.photoStorageKey
    if (person.photoStorageKey) {
      try {
        setPhotoPreview(URL.createObjectURL(await personApi.getPhoto(person.id)))
      } catch {
        photoStorageKey.value = person.photoStorageKey
      }
    }
    contacts.value = person.contacts.map((contact) => ({
      key: nextContactKey++,
      type: contact.type,
      value: contact.value,
      primary: contact.primary,
      sortOrder: contact.sortOrder,
      showContact: contact.showContact !== false,
    }))
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('personForm.loadError')
  } finally {
    loadingPerson.value = false
  }
})

onBeforeUnmount(() => {
  if (photoObjectUrl) URL.revokeObjectURL(photoObjectUrl)
})

function setPhotoPreview(url: string) {
  if (photoObjectUrl) URL.revokeObjectURL(photoObjectUrl)
  photoObjectUrl = url
  photoPreview.value = url
}

function selectPhoto(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  ;(event.target as HTMLInputElement).value = ''
  if (!file) return
  selectedPhoto.value = file
  setPhotoPreview(URL.createObjectURL(file))
}

function required(value: string) {
  return Boolean(value?.trim()) || translate('personForm.required')
}

function maxLength(maximum: number) {
  return (value: string) => !value || value.length <= maximum || translate('personForm.maxLength', { maximum })
}

function addContact() {
  contacts.value.push({
    key: nextContactKey++,
    type: 'EMAIL',
    value: '',
    primary: contacts.value.length === 0,
    sortOrder: contacts.value.length,
    showContact: true,
  })
}

function removeContact(index: number) {
  contacts.value.splice(index, 1)
  contacts.value.forEach((contact, order) => { contact.sortOrder = order })
}

async function savePerson() {
  error.value = ''
  saving.value = true
  const input = {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    dateOfBirth: form.dateOfBirth || null,
    position: form.position.trim() || null,
    gender: form.gender || null,
    maritalStatus: form.maritalStatus || null,
    militaryStatus: form.militaryStatus || null,
    location: form.location.trim() || null,
    photoStorageKey: photoStorageKey.value,
    contacts: contacts.value.map(({ key: _key, ...contact }) => contact),
  }

  try {
    const editing = isEditing.value
    let personId: string
    if (editing) {
      personId = String(route.params.id)
      await store.updatePerson(personId, input)
    } else {
      const person = await store.createPerson(input)
      personId = person.id
      if (selectedPhoto.value) {
        await router.replace(`/people/${personId}/edit`)
      }
    }
    if (selectedPhoto.value) {
      const person = await store.uploadPersonPhoto(personId, selectedPhoto.value)
      photoStorageKey.value = person.photoStorageKey
      selectedPhoto.value = null
    }
    await router.push('/')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('personForm.saveError')
  } finally {
    saving.value = false
  }
}
</script>