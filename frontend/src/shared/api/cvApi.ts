import apiClient from './axios'
import type { Cv, CvInput } from './cvTypes'

const cvApi = {
  async list(personId?: string): Promise<Cv[]> {
    const response = await apiClient.get<Cv[]>('/cvs', {
      params: personId ? { personId } : undefined,
    })
    return response.data
  },

  async get(id: string): Promise<Cv> {
    const response = await apiClient.get<Cv>(`/cvs/${id}`)
    return response.data
  },

  async create(personId: string, input: CvInput): Promise<Cv> {
    const response = await apiClient.post<Cv>(`/persons/${personId}/cvs`, input)
    return response.data
  },

  async update(id: string, input: CvInput): Promise<Cv> {
    const response = await apiClient.put<Cv>(`/cvs/${id}`, input)
    return response.data
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/cvs/${id}`)
  },
}

export default cvApi