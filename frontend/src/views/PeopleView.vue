<template>
  <main class="people-view">
    <div class="page-heading">
      <div>
        <div class="eyebrow">PEOPLE DIRECTORY</div>
        <h1>People</h1>
        <p>Manage profiles and the contact details connected to their CVs.</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-plus" rounded="lg" to="/people/new">
        Add person
      </v-btn>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>

    <div class="directory-toolbar">
      <v-text-field
        v-model="search"
        aria-label="Search people"
        density="compact"
        hide-details
        label="Search people"
        prepend-inner-icon="mdi-magnify"
        variant="outlined"
        class="directory-search"
      />
      <span class="directory-count">{{ people.length }} {{ people.length === 1 ? 'person' : 'people' }}</span>
    </div>

    <v-progress-linear v-if="store.loading" color="primary" indeterminate />

    <div v-else-if="people.length === 0" class="people-empty">
      <v-icon icon="mdi-account-plus-outline" size="30" />
      <h2>No people yet</h2>
      <p>Create the first profile to start building a CV library.</p>
      <v-btn color="primary" prepend-icon="mdi-plus" to="/people/new">Add person</v-btn>
    </div>

    <v-data-table
      v-else
      class="people-table"
      :headers="headers"
      :items="people"
      :search="search"
      item-value="id"
      :items-per-page="10"
    >
      <template #item.name="{ item }">
        <RouterLink class="person-name-link" :to="`/people/${item.id}/edit`">
          {{ item.firstName }} {{ item.lastName }}
        </RouterLink>
      </template>
      <template #item.position="{ item }">
        <span class="muted-cell">{{ item.position || '—' }}</span>
      </template>
      <template #item.contacts="{ item }">
        <span class="contact-count">{{ item.contacts.length }}</span>
        <span class="muted-cell">{{ item.contacts.length === 1 ? ' contact' : ' contacts' }}</span>
      </template>
      <template #item.actions="{ item }">
        <div class="row-actions">
          <v-tooltip text="View CVs">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`View CVs for ${item.firstName} ${item.lastName}`" icon="mdi-file-document-multiple-outline" size="small" variant="text" :to="`/people/${item.id}/cvs`" />
            </template>
          </v-tooltip>
          <v-tooltip text="Edit person">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Edit ${item.firstName} ${item.lastName}`" icon="mdi-pencil-outline" size="small" variant="text" :to="`/people/${item.id}/edit`" />
            </template>
          </v-tooltip>
          <v-tooltip text="Delete person">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="`Delete ${item.firstName} ${item.lastName}`" color="error" icon="mdi-trash-can-outline" size="small" variant="text" @click="openDelete(item)" />
            </template>
          </v-tooltip>
        </div>
      </template>
      <template #no-data>
        <div class="table-empty">No people match “{{ search }}”.</div>
      </template>
    </v-data-table>

    <v-dialog v-model="deleteDialog" max-width="430">
      <v-card>
        <v-card-title class="dialog-title">Delete person?</v-card-title>
        <v-card-text>
          This will permanently delete {{ selectedPerson?.firstName }} {{ selectedPerson?.lastName }} and their contact details.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">Cancel</v-btn>
          <v-btn color="error" :loading="deleting" @click="confirmDelete">Delete</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { usePersonStore } from '../shared/stores/personStore'
import type { Person } from '../shared/api/personTypes'

const store = usePersonStore()
const people = computed(() => store.people)
const search = ref('')
const error = ref('')
const deleteDialog = ref(false)
const deleting = ref(false)
const selectedPerson = ref<Person | null>(null)
const headers = [
  { title: 'Name', key: 'name' },
  { title: 'Position', key: 'position' },
  { title: 'Contacts', key: 'contacts', sortable: false },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
]

onMounted(async () => {
  try {
    await store.fetchPeople()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to load people.'
  }
})

function openDelete(person: Person) {
  selectedPerson.value = person
  deleteDialog.value = true
}

async function confirmDelete() {
  if (!selectedPerson.value) return
  deleting.value = true
  error.value = ''
  try {
    await store.deletePerson(selectedPerson.value.id)
    deleteDialog.value = false
    selectedPerson.value = null
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to delete this person.'
  } finally {
    deleting.value = false
  }
}
</script>