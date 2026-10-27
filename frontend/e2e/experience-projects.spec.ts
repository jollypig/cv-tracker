import { expect, test } from '@playwright/test'
import type { CvContent, CvExperienceProject } from '../src/shared/api/cvTypes'

function project(projectName: string, sortOrder: number): CvExperienceProject {
  return {
    projectKey: projectName, projectName, sortOrder, company: '', industries: '',
    projectDescription: '', periodFrom: null, periodTo: null, position: '',
    responsibilities: '', technologies: '', teamSize: null, externalLink: '',
  }
}

for (const width of [1440, 390]) {
  test(`reorders and inserts experience projects at either end at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 })
    const cv = { id: 'target', personId: 'person', personName: 'Alex Example', name: 'Target CV',
      language: 'en', status: 'DRAFT', tags: [], templateId: null, currentVersionId: null }
    let content: CvContent = {
      summary: '', experiences: [
        { company: 'Example Company', position: 'Developer', location: '', employmentType: null,
          employmentLocation: null, startDate: null, endDate: null, current: false, description: '',
          sortOrder: 0, projects: [project('Alpha', 0), project('Beta', 1)] },
        { company: 'Second Company', position: 'Developer', location: '', employmentType: null,
          employmentLocation: null, startDate: null, endDate: null, current: false, description: '',
          sortOrder: 1, projects: [] },
      ], education: [], skillGroups: [], languages: [], projects: [], certifications: [],
      customSections: [], sections: [],
    }
    let saves = 0
    await page.route('**/api/v1/**', async (route) => {
      const path = new URL(route.request().url()).pathname
      let body: unknown = {}
      if (path.endsWith('/auth/me')) body = { id: 'owner', displayName: 'Alex Example' }
      else if (path.endsWith('/auth/config')) body = { oidcEnabled: true }
      else if (path.endsWith('/content')) {
        if (route.request().method() === 'PUT') {
          content = route.request().postDataJSON()
          saves++
        }
        body = content
      } else if (path.endsWith('/templates')) body = []
      else if (path.endsWith('/persons/person')) body = { id: 'person', firstName: 'Alex', lastName: 'Example', contacts: [] }
      else if (path.endsWith('/cvs/target')) body = cv
      await route.fulfill({ json: body })
    })

    await page.goto('/cvs/target/content')
    await page.getByRole('button', { name: 'Experience 2', exact: true }).click()
    const positions = page.locator('.experience-entry-row')
    const position = positions.first()
    await position.getByText('Example Company', { exact: true }).click()
    const rows = position.locator('.experience-project-row')
    const headings = rows.locator('.v-expansion-panel-title')
    await expect(headings).toHaveText([/Alpha/, /Beta/])
    await expect(rows.first().getByRole('button', { name: 'Move project up', exact: true })).toBeDisabled()
    await expect(rows.last().getByRole('button', { name: 'Move project down', exact: true })).toBeDisabled()

    await rows.first().getByRole('button', { name: /Alpha/ }).click()
    await rows.first().getByRole('button', { name: 'Move project down', exact: true }).click()
    await expect(headings).toHaveText([/Beta/, /Alpha/])
    await expect(rows.last().getByLabel('Project name', { exact: true })).toBeVisible()
    await rows.last().getByRole('button', { name: 'Move project up', exact: true }).click()
    await expect(headings).toHaveText([/Alpha/, /Beta/])

    const addButtons = position.getByRole('button', { name: 'Add project', exact: true })
    await addButtons.first().click()
    await expect(headings).toHaveText([/Project 1/, /Alpha/, /Beta/])
    await expect(rows.first().getByLabel('Project name', { exact: true })).toBeVisible()
    await rows.first().getByLabel('Project name', { exact: true }).fill('Top project')
    await addButtons.last().click()
    await expect(headings).toHaveText([/Top project/, /Alpha/, /Beta/, /Project 4/])
    await expect(rows.last().getByLabel('Project name', { exact: true })).toBeVisible()
    await rows.last().getByLabel('Project name', { exact: true }).fill('Bottom project')
    await rows.last().locator('.v-expansion-panel-title').click()
    await expect(rows.last().getByLabel('Project name', { exact: true })).not.toBeVisible()
    await rows.last().locator('.v-expansion-panel-title').click()
    await expect(rows.last().getByLabel('Project name', { exact: true })).toBeVisible()
    await rows.last().getByRole('button', { name: 'Move project up', exact: true }).click()
    await expect(headings).toHaveText([/Top project/, /Alpha/, /Bottom project/, /Beta/])

    const second = positions.last()
    await second.getByText('Second Company', { exact: true }).click()
    await expect(second.getByRole('button', { name: 'Add project', exact: true })).toHaveCount(1)
    await second.getByRole('button', { name: 'Add project', exact: true }).click()
    const secondProject = second.locator('.experience-project-row')
    await expect(secondProject.getByLabel('Project name', { exact: true })).toBeVisible()
    await secondProject.getByLabel('Project name', { exact: true }).fill('Only project')
    await expect(secondProject.getByRole('button', { name: 'Move project up', exact: true })).toBeDisabled()
    await expect(secondProject.getByRole('button', { name: 'Move project down', exact: true })).toBeDisabled()
    await expect(headings).toHaveText([/Top project/, /Alpha/, /Bottom project/, /Beta/])

    await page.locator('.content-editor').getByRole('button', { name: 'Save content', exact: true }).click()
    await expect.poll(() => saves).toBe(1)
    expect(content.experiences[0]!.projects.map(({ projectName, sortOrder }) => ({ projectName, sortOrder })))
      .toEqual(['Top project', 'Alpha', 'Bottom project', 'Beta'].map((projectName, sortOrder) => ({ projectName, sortOrder })))
    expect(content.experiences[0]!.projects.find((item) => item.projectName === 'Alpha')!.projectKey).toBe('Alpha')
    await page.reload()
    await page.getByRole('button', { name: 'Experience 2', exact: true }).click()
    await position.getByText('Example Company', { exact: true }).click()
    await expect(headings).toHaveText([/Top project/, /Alpha/, /Bottom project/, /Beta/])
  })
}
