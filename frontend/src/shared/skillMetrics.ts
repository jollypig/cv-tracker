import type { CvContent, CvSkill, CvSkillDetails } from './api/cvTypes'

export function emptySkillDetails(): CvSkillDetails {
  return { yearsOfExperience: null, yearsActivelyUsed: null, lastUsed: null, startedFrom: null,
    frequency: null, status: null, linkedProjects: [], includeInOutput: false }
}

export function skillDate(value: string | null | undefined): string | null {
  if (!value) return null
  if (!/^\d{4}(-\d{2}-\d{2})?$/.test(value)) return null
  const iso = value.length === 4 ? `${value}-01-01` : value
  const date = new Date(`${iso}T00:00:00Z`)
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === iso ? iso : null
}

export function skillProjects(content: CvContent) {
  return [
    ...content.experiences.flatMap((experience) => experience.projects.map((project) => ({
      key: project.projectKey, title: `${project.projectName} (${experience.company})`,
      description: project.projectDescription ?? '',
      outputTitle: project.showProjectName === false ? 'Project' : project.projectName,
      start: project.periodFrom, end: project.periodTo, current: !project.periodTo && experience.current,
    }))),
    ...content.projects.map((project) => ({ key: project.projectKey, title: project.name,
      description: project.description ?? '',
      outputTitle: project.name, start: project.periodFrom, end: project.periodTo, current: project.current })),
  ]
}

export function matchesSkillProject(project: { title: string; description?: string | null } | undefined, query: string) {
  if (!project) return false
  const text = `${project.title} ${project.description ?? ''}`.toLocaleLowerCase()
  return query.trim().toLocaleLowerCase().split(/\s+/).every((term) => text.includes(term))
}

export function selectSkillProjects(skill: CvSkill, projectKeys: string[]) {
  if (!skill.details) return
  const existing = new Map(skill.details.linkedProjects.map((link) => [link.projectKey, link]))
  skill.details.linkedProjects = [...new Set(projectKeys)].map((projectKey) =>
    existing.get(projectKey) ?? { projectKey, outcome: '' })
}

export function calculateSkill(skill: CvSkill, content: CvContent, today = localToday()) {
  const details = skill.details
  let lastUsed = skillDate(details?.lastUsed)
  let days = 0
  const keys = new Set(details?.linkedProjects?.map((link) => link.projectKey) ?? [])
  for (const project of skillProjects(content)) {
    if (!project.key || !keys.has(project.key)) continue
    const end = project.current ? today : project.end
    if (end && (!lastUsed || end > lastUsed)) lastUsed = end
    if (project.start && end && end >= project.start) {
      days += (Date.parse(end) - Date.parse(project.start)) / 86400000
    }
  }
  const toYears = (duration: number) => Math.round(duration / 365.2425 * 100) / 100
  const start = skillDate(details?.startedFrom)
  const explicit = details?.yearsOfExperience
  const yearsOfExperience = explicit !== null && explicit !== undefined
    ? Number(explicit) : start && lastUsed && lastUsed >= start ? toYears((Date.parse(lastUsed) - Date.parse(start)) / 86400000) : null
  const totalExperience = days === 0 ? yearsOfExperience : Math.max(yearsOfExperience ?? 0, toYears(days))
  const threshold = new Date(`${today}T00:00:00Z`)
  threshold.setUTCFullYear(threshold.getUTCFullYear() - 5)
  return { yearsOfExperience: yearsOfExperience === null ? null : Math.ceil(yearsOfExperience),
    totalExperience: totalExperience === null ? null : Math.ceil(totalExperience), lastUsed,
    stale: Boolean(lastUsed && lastUsed < threshold.toISOString().slice(0, 10)) }
}

function localToday() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export function skillOutput(skill: CvSkill, content: CvContent): string[] {
  const details = skill.details
  if (!content.includeSkillDetailsInOutput || !details) return []
  const calculated = calculateSkill(skill, content)
  const values: string[] = []
  if (calculated.yearsOfExperience !== null) values.push(`Years of experience: ${calculated.yearsOfExperience}`)
  if (calculated.totalExperience !== null) values.push(`Total experience: ${calculated.totalExperience} years`)
  if (details.yearsActivelyUsed !== null) values.push(`Actively used: ${details.yearsActivelyUsed} years`)
  if (details.startedFrom) values.push(`Started from: ${details.startedFrom}`)
  if (calculated.lastUsed) values.push(`Last used: ${calculated.lastUsed}`)
  if (details.frequency) values.push(details.frequency)
  if (details.status) values.push(details.status)
  if (calculated.stale) values.push('Not used in over 5 years')
  const lines = values.length ? [`${skill.name}: ${values.join(' | ')}`] : []
  for (const link of details.linkedProjects ?? []) {
    const project = skillProjects(content).find((item) => item.key === link.projectKey)
    if (project) lines.push(`${project.outputTitle}${link.outcome ? `: ${link.outcome}` : ''}`)
  }
  return lines
}