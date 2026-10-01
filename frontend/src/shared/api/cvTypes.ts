export type CvStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'

export interface Cv {
  id: string
  personId: string
  personName: string
  name: string
  description: string | null
  language: string
  status: CvStatus
  templateId: string | null
  currentVersionId: string | null
  createdAt: string
  updatedAt: string
}

export interface CvInput {
  name: string
  description: string | null
  language: string
  status: CvStatus
}