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
  tags: string[]
  createdAt: string
  updatedAt: string
}

export interface CvInput {
  name: string
  description: string | null
  language: string
  status: CvStatus
  tags: string[]
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
  parentVersionId: string | null
  parentCvId: string | null
  parentVersionNumber: number | null
}

export type CvVersionChangeType = 'ADDED' | 'REMOVED' | 'MODIFIED'

export interface CvVersionChange {
  path: string
  type: CvVersionChangeType
  oldValue: unknown
  newValue: unknown
}

export interface CvVersionDiff {
  fromVersion: CvVersion
  toVersion: CvVersion
  changes: CvVersionChange[]
}

export interface CvVersionSnapshot {
  templateId: string | null
  name: string
  description: string | null
  language: string
  status: CvStatus
  tags?: string[]
  content: CvContent
}

export interface ExtractedValue<T> {
  value: T
  confidence: number | null
  sourceText: string | null
}

export interface ParsedCvDocument {
  personalData: {
    firstName: ExtractedValue<string> | null
    lastName: ExtractedValue<string> | null
    email: ExtractedValue<string> | null
    phone: ExtractedValue<string> | null
    location: ExtractedValue<string> | null
    urls: ExtractedValue<string>[]
  } | null
  professionalSummary: ExtractedValue<string> | null
  employment: {
    company: ExtractedValue<string> | null
    position: ExtractedValue<string> | null
    startDate: ExtractedValue<string> | null
    endDate: ExtractedValue<string> | null
    location: ExtractedValue<string> | null
    employmentType: ExtractedValue<string> | null
    industry: ExtractedValue<string> | null
    projects: ParsedCvDocumentProject[]
  }[]
  projects: ParsedCvDocumentProject[]
  education: {
    institution: ExtractedValue<string> | null
    degree: ExtractedValue<string> | null
    fieldOfStudy: ExtractedValue<string> | null
    startDate: ExtractedValue<string> | null
    endDate: ExtractedValue<string> | null
    description: ExtractedValue<string> | null
  }[]
  languages: {
    name: ExtractedValue<string> | null
    proficiency: ExtractedValue<string> | null
  }[]
  skills: {
    name: ExtractedValue<string> | null
    group: ExtractedValue<string> | null
    evidence: ExtractedValue<string>[]
    canonicalName: string | null
    yearsOfExperience: number | null
    lastUsedDate: string | null
    requiresReview: boolean
  }[]
  warnings: string[]
}

export interface ParsedCvDocumentProject {
  company: ExtractedValue<string> | null
  industries: ExtractedValue<string>[]
  projectName: ExtractedValue<string> | null
  projectDescription: ExtractedValue<string> | null
  startDate: ExtractedValue<string> | null
  endDate: ExtractedValue<string> | null
  position: ExtractedValue<string> | null
  responsibilities: ExtractedValue<string>[]
  technologiesAndTools: ExtractedValue<string>[]
}

export type CvDocumentImportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'NEEDS_REVIEW' | 'APPROVED' | 'FAILED'

export interface CvDocumentImportResponse {
  importId: string
  fileName: string
  mediaType: string
  fileSize: number
  status: CvDocumentImportStatus
  errorMessage: string | null
  result: ParsedCvDocument | null
  cvId: string | null
  createdAt: string
  updatedAt: string
}

export interface CvDocumentImportApproval {
  personId: string
  name: string
  language: string
  status: CvStatus
  tags: string[]
}

export interface CvVersionDetail extends CvVersion {
  snapshot: CvVersionSnapshot
}

export interface CvShareStatus {
  enabled: boolean
  createdAt: string | null
  viewCount: number
  lastViewedAt: string | null
}

export interface CvShareLink {
  url: string
  createdAt: string
}

export interface CvExperience {
  company: string
  position: string
  location: string | null
  employmentType: 'Full-time' | 'Part-time' | 'Contract' | 'Freelance' | 'Internship' | 'Self-employed' | null
  employmentLocation: 'On-site' | 'Hybrid' | 'Remote' | null
  startDate: string | null
  endDate: string | null
  current: boolean
  description: string | null
  sortOrder: number
  projects: CvExperienceProject[]
}

export interface CvExperienceProject {
  projectKey?: string | null
  company: string | null
  industries: string | null
  projectName: string
  projectDescription: string | null
  showProjectName?: boolean
  showCustomerCompany?: boolean
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
  current: boolean
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
  visible?: boolean
  details?: CvSkillDetails | null
}

export interface CvSkillDetails {
  yearsOfExperience: number | null
  yearsActivelyUsed: number | null
  lastUsed: string | null
  startedFrom: string | null
  frequency: 'daily' | 'occasionally' | 'rarely' | null
  status: 'active' | 'learning' | 'maintaining' | 'deprecated' | null
  linkedProjects: { projectKey: string; outcome: string | null }[]
  includeInOutput: boolean
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
  projectKey?: string | null
  periodFrom?: string | null
  periodTo?: string | null
  current?: boolean
  name: string
  role: string | null
  description: string | null
  technologies: string | null
  url: string | null
  sortOrder: number
}

export interface CvCertification {
  name: string
  description: string | null
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