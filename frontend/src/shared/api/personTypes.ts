export type ContactType = 'EMAIL' | 'PHONE' | 'LINKEDIN' | 'GITHUB' | 'WEBSITE' | 'FACEBOOK' | 'WHATSAPP' | 'VIBER' | 'TELEGRAM' | 'INSTAGRAM' | 'ADDRESS' | 'OTHER'

export interface PersonContact {
  id: string
  type: ContactType
  value: string
  primary: boolean
  sortOrder: number
  showContact: boolean
}

export interface PersonContactInput {
  type: ContactType
  value: string
  primary: boolean
  sortOrder: number
  showContact: boolean
}

export interface Person {
  id: string
  firstName: string
  lastName: string
  dateOfBirth: string | null
  position: string | null
  gender: string | null
  maritalStatus: string | null
  militaryStatus: string | null
  location: string | null
  photoStorageKey: string | null
  contacts: PersonContact[]
  createdAt: string
  updatedAt: string
}

export interface PersonInput {
  firstName: string
  lastName: string
  dateOfBirth: string | null
  position: string | null
  gender: string | null
  maritalStatus: string | null
  militaryStatus: string | null
  location: string | null
  photoStorageKey: string | null
  contacts: PersonContactInput[]
}