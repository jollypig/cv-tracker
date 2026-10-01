export type ContactType = 'EMAIL' | 'PHONE' | 'LINKEDIN' | 'GITHUB' | 'WEBSITE' | 'ADDRESS' | 'OTHER'

export interface PersonContact {
  id: string
  type: ContactType
  value: string
  primary: boolean
  sortOrder: number
}

export interface PersonContactInput {
  type: ContactType
  value: string
  primary: boolean
  sortOrder: number
}

export interface Person {
  id: string
  firstName: string
  lastName: string
  dateOfBirth: string | null
  headline: string | null
  photoStorageKey: string | null
  contacts: PersonContact[]
  createdAt: string
  updatedAt: string
}

export interface PersonInput {
  firstName: string
  lastName: string
  dateOfBirth: string | null
  headline: string | null
  photoStorageKey: string | null
  contacts: PersonContactInput[]
}