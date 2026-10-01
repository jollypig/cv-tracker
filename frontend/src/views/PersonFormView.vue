<template>
  <main class="person-form-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">PEOPLE DIRECTORY</div>
        <h1>{{ isEditing ? 'Edit person' : 'Add person' }}</h1>
        <p>{{ isEditing ? 'Update profile details and contact methods.' : 'Create a profile for someone in your workspace.' }}</p>
      </div>
      <v-btn icon="mdi-arrow-left" aria-label="Back to people" to="/" variant="text" />
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal">
      {{ error }}
    </v-alert>
    <v-progress-linear v-if="loadingPerson" class="form-loading" color="primary" indeterminate />

    <v-form v-else @submit.prevent="savePerson">
      <section class="form-section" aria-labelledby="profile-section-title">
        <div class="form-section-heading">
          <h2 id="profile-section-title">Profile</h2>
          <p>Required fields are marked in the form.</p>
        </div>
        <v-row>
          <v-col cols="12" sm="6">
            <v-text-field
              v-model="form.firstName"
              :rules="[required, maxLength(100)]"
              label="First name"
              maxlength="100"
              required
            />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field
              v-model="form.lastName"
              :rules="[required, maxLength(100)]"
              label="Last name"
              maxlength="100"
              required
            />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field v-model="form.headline" label="Professional headline" maxlength="255" />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field v-model="form.dateOfBirth" label="Date of birth" type="date" />
          </v-col>
        </v-row>
      </section>

      <section class="form-section contacts-section" aria-labelledby="contacts-section-title">
        <div class="form-section-heading contacts-heading">
          <div>
            <h2 id="contacts-section-title">Contact details</h2>
            <p>Email, phone, and professional links.</p>
          </div>
          <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addContact">Add contact</v-btn>
        </div>

        <div v-if="contacts.length === 0" class="contacts-empty">No contact details added.</div>
        <div v-for="(contact, index) in contacts" :key="contact.key" class="contact-row">
          <v-select
            v-model="contact.type"
            :items="contactTypes"
            label="Type"
            item-title="title"
            item-value="value"
            class="contact-type-field"
          />
          <v-text-field
            v-model="contact.value"
            :rules="[required, maxLength(500)]"
            label="Contact value"
            maxlength="500"
            required
          />
          <v-checkbox v-model="contact.primary" hide-details label="Primary" />
          <v-tooltip text="Remove contact">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Remove contact ${index + 1}`" class="remove-contact" color="error" icon="mdi-close" variant="text" @click="removeContact(index)" />
            </template>
          </v-tooltip>
        </div>
      </section>

      <div class="form-actions">
        <v-btn variant="text" to="/">Cancel</v-btn>
        <v-btn color="primary" :loading="saving" type="submit">
          {{ isEditing ? 'Save changes' : 'Create person' }}
        </v-btn>
      </div>
    </v-form>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePersonStore } from '../shared/stores/personStore'
import type { ContactType, PersonContactInput } from '../shared/api/personTypes'

const route = useRoute()
const router = useRouter()
const store = usePersonStore()
const isEditing = computed(() => typeof route.params.id === 'string')
const loadingPerson = ref(false)
const saving = ref(false)
const error = ref('')
const form = reactive({ firstName: '', lastName: '', dateOfBirth: '', headline: '' })
const contacts = ref<Array<PersonContactInput & { key: number }>>([])
let nextContactKey = 0

const contactTypes: Array<{ title: string; value: ContactType }> = [
  { title: 'Email', value: 'EMAIL' },
  { title: 'Phone', value: 'PHONE' },
  { title: 'LinkedIn', value: 'LINKEDIN' },
  { title: 'GitHub', value: 'GITHUB' },
  { title: 'Website', value: 'WEBSITE' },
  { title: 'Address', value: 'ADDRESS' },
  { title: 'Other', value: 'OTHER' },
]

onMounted(async () => {
  if (!isEditing.value) return
  loadingPerson.value = true
  try {
    const person = await store.fetchPerson(String(route.params.id))
    form.firstName = person.firstName
    form.lastName = person.lastName
    form.dateOfBirth = person.dateOfBirth ?? ''
    form.headline = person.headline ?? ''
    contacts.value = person.contacts.map((contact) => ({
      key: nextContactKey++,
      type: contact.type,
      value: contact.value,
      primary: contact.primary,
      sortOrder: contact.sortOrder,
    }))
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to load this person.'
  } finally {
    loadingPerson.value = false
  }
})

function required(value: string) {
  return Boolean(value?.trim()) || 'This field is required.'
}

function maxLength(maximum: number) {
  return (value: string) => !value || value.length <= maximum || `Use ${maximum} characters or fewer.`
}

function addContact() {
  contacts.value.push({
    key: nextContactKey++,
    type: 'EMAIL',
    value: '',
    primary: contacts.value.length === 0,
    sortOrder: contacts.value.length,
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
    headline: form.headline.trim() || null,
    photoStorageKey: null,
    contacts: contacts.value.map(({ key: _key, ...contact }) => contact),
  }

  try {
    if (isEditing.value) {
      await store.updatePerson(String(route.params.id), input)
    } else {
      await store.createPerson(input)
    }
    await router.push('/')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to save this person.'
  } finally {
    saving.value = false
  }
}
</script>