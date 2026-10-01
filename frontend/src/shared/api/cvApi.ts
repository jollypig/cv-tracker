import apiClient from './axios'
import type { Cv, CvContent, CvInput } from './cvTypes'

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

  async getContent(id: string): Promise<CvContent> {
    const response = await apiClient.get<CvContent>(`/cvs/${id}/content`)
    return response.data
  },

  async saveContent(id: string, content: CvContent): Promise<CvContent> {
    const response = await apiClient.put<CvContent>(`/cvs/${id}/content`, content)
    return response.data
  },
}

export default cvApi