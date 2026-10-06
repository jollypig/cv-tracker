import { defineStore } from 'pinia'
import { ref } from 'vue'
import personApi from '../api/personApi'
import type { Person, PersonInput } from '../api/personTypes'

export const usePersonStore = defineStore('people', () => {
  const people = ref<Person[]>([])
  const loading = ref(false)

  async function fetchPeople() {
    loading.value = true
    try {
      people.value = await personApi.list()
    } finally {
      loading.value = false
    }
  }

  async function fetchPerson(id: string) {
    return personApi.get(id)
  }

  async function createPerson(input: PersonInput) {
    const person = await personApi.create(input)
    people.value = [...people.value, person]
    return person
  }

  async function updatePerson(id: string, input: PersonInput) {
    const person = await personApi.update(id, input)
    people.value = people.value.map((current) => current.id === id ? person : current)
    return person
  }

  async function uploadPersonPhoto(id: string, file: File) {
    const person = await personApi.uploadPhoto(id, file)
    people.value = people.value.map((current) => current.id === id ? person : current)
    return person
  }

  async function deletePerson(id: string) {
    await personApi.remove(id)
    people.value = people.value.filter((person) => person.id !== id)
  }

  return { people, loading, fetchPeople, fetchPerson, createPerson, updatePerson, uploadPersonPhoto, deletePerson }
})