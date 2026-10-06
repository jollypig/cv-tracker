import apiClient from './axios'
import type { Person, PersonInput } from './personTypes'

const personApi = {
  async list(): Promise<Person[]> {
    const response = await apiClient.get<Person[]>('/persons')
    return response.data
  },

  async get(id: string): Promise<Person> {
    const response = await apiClient.get<Person>(`/persons/${id}`)
    return response.data
  },

  async create(input: PersonInput): Promise<Person> {
    const response = await apiClient.post<Person>('/persons', input)
    return response.data
  },

  async update(id: string, input: PersonInput): Promise<Person> {
    const response = await apiClient.put<Person>(`/persons/${id}`, input)
    return response.data
  },

  async uploadPhoto(id: string, file: File): Promise<Person> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.post<Person>(`/persons/${id}/photo`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  },

  async getPhoto(id: string): Promise<Blob> {
    const response = await apiClient.get<Blob>(`/persons/${id}/photo`, { responseType: 'blob' })
    return response.data
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/persons/${id}`)
  },
}

export default personApi