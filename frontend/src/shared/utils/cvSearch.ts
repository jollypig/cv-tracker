import type { Cv } from '../api/cvTypes'

function normalize(value: string): string {
  return value.normalize('NFKD').replace(/\p{M}/gu, '').toLocaleLowerCase()
}

export function matchesCvSearch(cv: Cv, query: string): boolean {
  const terms = normalize(query).trim().split(/\s+/).filter(Boolean)
  if (terms.length === 0) return true

  const searchableText = normalize([
    cv.name,
    cv.personName,
    cv.description ?? '',
    cv.language,
    cv.status,
    ...cv.tags,
  ].join(' '))

  return terms.every((term) => searchableText.includes(term))
}