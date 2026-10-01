import { defineStore } from 'pinia'
import { ref } from 'vue'
import cvApi from '../api/cvApi'
import type { Cv, CvInput } from '../api/cvTypes'

export const useCvStore = defineStore('cvs', () => {
  const cvs = ref<Cv[]>([])
  const loading = ref(false)

  async function fetchCvs(personId?: string) {
    loading.value = true
    try {
      cvs.value = await cvApi.list(personId)
    } finally {
      loading.value = false
    }
  }

  async function fetchCv(id: string) {
    return cvApi.get(id)
  }

  async function createCv(personId: string, input: CvInput) {
    const cv = await cvApi.create(personId, input)
    cvs.value = [cv, ...cvs.value]
    return cv
  }

  async function updateCv(id: string, input: CvInput) {
    const cv = await cvApi.update(id, input)
    cvs.value = cvs.value.map((current) => current.id === id ? cv : current)
    return cv
  }

  async function deleteCv(id: string) {
    await cvApi.remove(id)
    cvs.value = cvs.value.filter((cv) => cv.id !== id)
  }

  return { cvs, loading, fetchCvs, fetchCv, createCv, updateCv, deleteCv }
})