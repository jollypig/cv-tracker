import apiClient from './axios'
import type { Cv, CvContent, CvInput, CvTemplate, CvVersion, CvVersionDetail } from './cvTypes'

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

  async selectTemplate(id: string, templateId: string): Promise<Cv> {
    const response = await apiClient.put<Cv>(`/cvs/${id}/template`, { templateId })
    return response.data
  },

  async listTemplates(): Promise<CvTemplate[]> {
    const response = await apiClient.get<CvTemplate[]>('/templates')
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

  async listVersions(id: string): Promise<CvVersion[]> {
    const response = await apiClient.get<CvVersion[]>(`/cvs/${id}/versions`)
    return response.data
  },

  async getVersion(id: string, versionNumber: number): Promise<CvVersionDetail> {
    const response = await apiClient.get<CvVersionDetail>(`/cvs/${id}/versions/${versionNumber}`)
    return response.data
  },

  async createVersion(id: string, description: string): Promise<CvVersion> {
    const response = await apiClient.post<CvVersion>(`/cvs/${id}/versions`, { description })
    return response.data
  },

  async restoreVersion(id: string, versionNumber: number, description: string): Promise<CvVersion> {
    const response = await apiClient.post<CvVersion>(`/cvs/${id}/versions/${versionNumber}/restore`, { description })
    return response.data
  },

  async exportVersionPdf(versionId: string): Promise<{ fileName: string; content: Blob }> {
    const created = await apiClient.post<{ id: string; fileName: string }>(`/cv-versions/${versionId}/exports/pdf`)
    const downloaded = await apiClient.get<Blob>(`/exports/${created.data.id}/download`, { responseType: 'blob' })
    return { fileName: created.data.fileName, content: downloaded.data }
  },
}

export default cvApi