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

export interface CvTemplate {
  id: string
  name: string
  description: string | null
  templateKey: string
  version: number
  active: boolean
  createdAt: string
}

export interface CvContent {
  summary: string | null
  experiences: CvExperience[]
  education: CvEducation[]
  skillGroups: CvSkillGroup[]
  languages: CvLanguage[]
  projects: CvProject[]
  certifications: CvCertification[]
  customSections: CvCustomSection[]
  sections: CvSection[]
}

export interface CvVersion {
  id: string
  cvId: string
  versionNumber: number
  description: string | null
  createdAt: string
}

export interface CvVersionSnapshot {
  templateId: string | null
  name: string
  description: string | null
  language: string
  status: CvStatus
  content: CvContent
}

export interface CvVersionDetail extends CvVersion {
  snapshot: CvVersionSnapshot
}

export interface CvExperience {
  company: string
  position: string
  location: string | null
  startDate: string | null
  endDate: string | null
  current: boolean
  description: string | null
  sortOrder: number
  projects: CvExperienceProject[]
}

export interface CvExperienceProject {
  company: string | null
  industries: string | null
  projectName: string
  projectDescription: string | null
  periodFrom: string | null
  periodTo: string | null
  position: string | null
  responsibilities: string | null
  technologies: string | null
  teamSize: number | null
  externalLink: string | null
  sortOrder: number
}

export interface CvEducation {
  institution: string
  degree: string | null
  diplomaDegreeWork: string | null
  fieldOfStudy: string | null
  startDate: string | null
  endDate: string | null
  description: string | null
  sortOrder: number
}

export interface CvSkillGroup {
  name: string
  sortOrder: number
  skills: CvSkill[]
}

export interface CvSkill {
  name: string
  level: string | null
  sortOrder: number
}

export interface CvLanguage {
  language: string
  level: string | null
  reading: string | null
  writing: string | null
  speaking: string | null
  sortOrder: number
}

export interface CvProject {
  name: string
  role: string | null
  description: string | null
  technologies: string | null
  url: string | null
  sortOrder: number
}

export interface CvCertification {
  name: string
  issuer: string | null
  issueDate: string | null
  expiryDate: string | null
  credentialId: string | null
  credentialUrl: string | null
  sortOrder: number
}

export interface CvCustomSection {
  title: string
  content: string | null
  sortOrder: number
}

export type CvSectionType = 'SUMMARY' | 'EXPERIENCE' | 'EDUCATION' | 'SKILLS' | 'LANGUAGES' | 'PROJECTS' | 'CERTIFICATIONS' | 'CUSTOM'

export interface CvSection {
  type: CvSectionType
  visible: boolean
  sortOrder: number
}