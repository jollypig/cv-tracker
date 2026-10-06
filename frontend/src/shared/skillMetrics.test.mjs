import assert from 'node:assert/strict'
import { test } from 'node:test'
import { calculateSkill, emptySkillDetails, matchesSkillProject, selectSkillProjects, skillDate, skillOutput, skillProjects } from './skillMetrics.ts'

const emptyContent = () => ({ experiences: [], projects: [] })
const skill = (details) => ({ name: 'Java', details: { ...emptySkillDetails(), ...details } })

test('rounds calculated experience upward and includes it when enabled for the CV', () => {
  const value = skill({ yearsOfExperience: 14.76, includeInOutput: true })
  const content = { ...emptyContent(), includeSkillDetailsInOutput: true }
  const result = calculateSkill(value, content, '2026-10-04')
  assert.equal(result.yearsOfExperience, 15)
  assert.equal(result.totalExperience, 15)
  assert.equal(value.details.yearsOfExperience, 14.76)
  assert.match(skillOutput(value, content)[0], /Total experience: 15 years/)
  const inferred = calculateSkill(skill({ startedFrom: '2010-01-01', lastUsed: '2024-10-04' }), emptyContent(), '2026-10-04')
  assert.equal(inferred.yearsOfExperience, 15)
  assert.equal(inferred.totalExperience, 15)
})

test('several skills can share several projects while preserving each link outcome', () => {
  const java = skill({ linkedProjects: [{ projectKey: 'platform', outcome: 'Delivered API' }] })
  const sql = skill({})
  selectSkillProjects(java, ['platform', 'analytics', 'platform'])
  selectSkillProjects(sql, ['platform', 'analytics'])
  assert.deepEqual(java.details.linkedProjects, [
    { projectKey: 'platform', outcome: 'Delivered API' }, { projectKey: 'analytics', outcome: '' },
  ])
  assert.deepEqual(sql.details.linkedProjects.map((link) => link.projectKey), ['platform', 'analytics'])
  java.details.linkedProjects[1].outcome = 'Improved reporting'
  selectSkillProjects(java, ['analytics'])
  assert.deepEqual(java.details.linkedProjects, [{ projectKey: 'analytics', outcome: 'Improved reporting' }])
  assert.equal(sql.details.linkedProjects.length, 2)
  selectSkillProjects(java, [])
  assert.deepEqual(java.details.linkedProjects, [])
})

test('project search matches names and descriptions across both project types', () => {
  const content = emptyContent()
  content.projects = [{ projectKey: 'platform', name: 'Platform', description: 'Payment processing service' }]
  content.experiences = [{ company: 'Company', projects: [
    { projectKey: 'analytics', projectName: 'Analytics', projectDescription: 'Warehouse reporting' },
  ] }]
  const projects = skillProjects(content)
  assert.equal(matchesSkillProject(projects[0], 'ANALYTICS'), true)
  assert.equal(matchesSkillProject(projects[0], 'warehouse'), true)
  assert.equal(matchesSkillProject(projects[1], ' platform PAYMENT '), true)
  assert.equal(matchesSkillProject(projects[1], 'reporting'), false)
  assert.equal(matchesSkillProject(projects[1], ''), true)
})

test('explicit years including zero take precedence; linked periods set total and latest use', () => {
  const content = emptyContent()
  content.projects.push({ projectKey: 'platform', name: 'Platform', periodFrom: '2020-01-01', periodTo: '2024-01-01' })
  const result = calculateSkill(skill({ yearsOfExperience: 2, lastUsed: '2016', startedFrom: '2010',
    linkedProjects: [{ projectKey: 'platform' }] }), content, '2026-10-04')
  assert.deepEqual(result, { yearsOfExperience: 2, totalExperience: 4, lastUsed: '2024-01-01', stale: false })
  assert.equal(calculateSkill(skill({ yearsOfExperience: 0, startedFrom: '2010', lastUsed: '2020' }), content).yearsOfExperience, 0)
})

test('infers experience from dates and flags old skills without inventing missing values', () => {
  assert.deepEqual(calculateSkill(skill({ startedFrom: '2010', lastUsed: '2018' }), emptyContent(), '2026-10-04'),
    { yearsOfExperience: 8, totalExperience: 8, lastUsed: '2018-01-01', stale: true })
  assert.equal(calculateSkill(skill({}), emptyContent(), '2026-10-04').totalExperience, null)
  assert.equal(skillDate('2024-02-30'), null)
  assert.equal(skillDate('2024-02-29'), '2024-02-29')
})

test('sums linked overlapping periods, ignores unlinked projects, and ends current projects today', () => {
  const content = emptyContent()
  content.projects = [
    { projectKey: 'first', periodFrom: '2020-01-01', periodTo: '2022-01-01' },
    { projectKey: 'second', periodFrom: '2021-01-01', current: true },
    { projectKey: 'unlinked', periodFrom: '2000-01-01', current: true },
  ]
  const result = calculateSkill(skill({ linkedProjects: [{ projectKey: 'first' }, { projectKey: 'second' },
    { projectKey: 'first' }] }), content, '2024-01-01')
  assert.equal(result.totalExperience, 5)
  assert.equal(result.lastUsed, '2024-01-01')
})

test('CV-wide output setting controls all skill details and honors hidden project names', () => {
  const content = emptyContent()
  content.experiences = [{ company: 'Company', current: false, projects: [
    { projectKey: 'hidden', projectName: 'Confidential', showProjectName: false },
  ] }]
  const value = skill({ linkedProjects: [{ projectKey: 'hidden', outcome: 'Delivered the platform' }] })
  content.includeSkillDetailsInOutput = false
  assert.deepEqual(skillOutput(value, content), [])
  content.includeSkillDetailsInOutput = true
  assert.deepEqual(skillOutput(value, content), ['Project: Delivered the platform'])
  value.details.includeInOutput = true
  content.includeSkillDetailsInOutput = false
  assert.deepEqual(skillOutput(value, content), [])
})