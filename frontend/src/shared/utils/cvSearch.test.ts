import { describe, expect, it } from 'vitest'
import { matchesCvSearch } from './cvSearch'
import type { Cv } from '../api/cvTypes'

const cv: Cv = {
  id: 'cv-id',
  personId: 'person-id',
  personName: 'José Alvarez',
  name: 'Platform Engineer',
  description: 'Cloud infrastructure specialist',
  language: 'en',
  status: 'ACTIVE',
  templateId: null,
  currentVersionId: null,
  tags: ['Kubernetes', 'Backend'],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('matchesCvSearch', () => {
  it('searches CV and person metadata, description, language, status, and tags', () => {
    for (const query of ['platform', 'alvarez', 'infrastructure', 'en', 'active', 'kubernetes']) {
      expect(matchesCvSearch(cv, query)).toBe(true)
    }
  })

  it('matches terms across fields without regard to case, accents, or order', () => {
    expect(matchesCvSearch(cv, 'ALVAREZ platform')).toBe(true)
    expect(matchesCvSearch(cv, 'jose backend')).toBe(true)
  })

  it('treats an empty query as a match and rejects unmatched terms', () => {
    expect(matchesCvSearch(cv, '   ')).toBe(true)
    expect(matchesCvSearch(cv, 'frontend')).toBe(false)
  })
})