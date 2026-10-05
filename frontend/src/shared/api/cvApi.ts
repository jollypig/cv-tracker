import apiClient from './axios'
import type { Cv, CvContent, CvDocumentImportApproval, CvDocumentImportResponse, CvInput, CvShareLink, CvShareStatus, CvTemplate, CvVersion, CvVersionDetail, CvVersionDiff, CvVersionSnapshot } from './cvTypes'

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

  async importSnapshot(personId: string, snapshot: CvVersionSnapshot): Promise<Cv> {
    const response = await apiClient.post<Cv>(`/persons/${personId}/cvs/import`, snapshot)
    return response.data
  },

  async startDocumentImport(file: File): Promise<CvDocumentImportResponse> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.post<CvDocumentImportResponse>('/cvs/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 900_000,
    })
    return response.data
  },

  async approveDocumentImport(importId: string, approval: CvDocumentImportApproval): Promise<CvDocumentImportResponse> {
    const response = await apiClient.post<CvDocumentImportResponse>(`/cvs/import/${importId}/approve`, approval)
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

  async duplicate(id: string, name: string, personId?: string, language?: string): Promise<Cv> {
    const response = await apiClient.post<Cv>(`/cvs/${id}/duplicate`, { name, personId, language })
    return response.data
  },

  async branchVersion(id: string, versionNumber: number, name: string): Promise<Cv> {
    const response = await apiClient.post<Cv>(`/cvs/${id}/versions/${versionNumber}/branch`, { name })
    return response.data
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

  async getVersionDiff(id: string, fromVersion: number, toVersion: number): Promise<CvVersionDiff> {
    const response = await apiClient.get<CvVersionDiff>(`/cvs/${id}/versions/diff`, {
      params: { fromVersion, toVersion },
    })
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

  async exportVersion(versionId: string, format: 'pdf' | 'docx' | 'json'): Promise<{ fileName: string; content: Blob }> {
    const created = await apiClient.post<{ id: string; fileName: string }>(`/cv-versions/${versionId}/exports/${format}`)
    const downloaded = await apiClient.get<Blob>(`/exports/${created.data.id}/download`, { responseType: 'blob' })
    return { fileName: created.data.fileName, content: downloaded.data }
  },

  async shareStatus(id: string): Promise<CvShareStatus> {
    const response = await apiClient.get<CvShareStatus>(`/cvs/${id}/share`)
    return response.data
  },

  async createShareLink(id: string): Promise<CvShareLink> {
    const response = await apiClient.put<CvShareLink>(`/cvs/${id}/share`)
    return response.data
  },

  async revokeShareLink(id: string): Promise<void> {
    await apiClient.delete(`/cvs/${id}/share`)
  },
}

export default cvApi