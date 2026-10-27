import { expect, test } from '@playwright/test'
import type { CvContent } from '../src/shared/api/cvTypes'

for (const viewport of [{ width: 1440, height: 900 }, { width: 390, height: 844 }]) {
  test(`preview preserves content when switching templates at ${viewport.width}px`, async ({ page }) => {
    await page.setViewportSize(viewport)
    const cv = { id: 'preview', personId: 'person', personName: 'Jane Doe', name: 'Backend CV',
      language: 'en', status: 'DRAFT', tags: [], templateId: null as string | null, currentVersionId: null }
    const templates = ['classic', 'minimal', 'modern'].map((key) => ({ id: key,
      name: key[0]!.toUpperCase() + key.slice(1), templateKey: key, active: true, version: 1 }))
    const content: CvContent = { summary: 'Profile summary', includeSkillDetailsInOutput: false,
      includeSkillLevelsInOutput: true, sections: [], education: [], skillGroups: [], languages: [],
      certifications: [], customSections: [],
      experiences: [{ company: 'Employer', position: 'Engineer', location: 'Riga office', employmentType: 'Contract',
        employmentLocation: 'Remote', startDate: '2020-01-01', endDate: null, current: true,
        description: 'Employment description', sortOrder: 0,
        projects: [{ projectName: 'Hidden project', showProjectName: false, company: 'Hidden customer',
          showCustomerCompany: false, position: 'Project lead', projectDescription: 'Nested description',
          responsibilities: 'Built APIs', technologies: 'Spring', sortOrder: 0, industries: null,
          periodFrom: null, periodTo: null, teamSize: null, externalLink: null }] }],
      projects: [{ name: 'Portfolio', role: 'Developer', description: 'Standalone description',
        technologies: 'Vue', url: 'https://example.com/project', sortOrder: 0 }] }
    await page.route('**/api/v1/**', async (route) => {
      const path = new URL(route.request().url()).pathname
      let body: unknown = {}
      if (path.endsWith('/auth/me')) body = { id: 'owner', displayName: 'Jane Doe' }
      else if (path.endsWith('/auth/config')) body = { oidcEnabled: true }
      else if (path.endsWith('/template')) {
        cv.templateId = route.request().postDataJSON().templateId
        body = cv
      } else if (path.endsWith('/templates')) body = templates
      else if (path.endsWith('/content')) body = content
      else if (path.endsWith('/persons/person')) body = { id: 'person', firstName: 'Jane', lastName: 'Doe',
        position: 'Engineer', location: 'Riga', contacts: [
          { id: 'hidden', type: 'EMAIL', value: 'hidden@example.com', sortOrder: 0, showContact: false },
          { id: 'visible', type: 'PHONE', value: '+371 20000000', sortOrder: 1, showContact: true }] }
      else if (path.endsWith('/cvs/preview')) body = cv
      else if (path.endsWith('/cvs')) body = [cv]
      await route.fulfill({ json: body })
    })

    await page.goto('/cvs/preview/content')
    const preview = page.locator('.cv-document')
    await expect(preview).toHaveClass(/cv-document--modern/)
    for (const template of templates) {
      await page.getByRole('button', { name: template.name, exact: true }).click()
      await expect(preview).toHaveClass(new RegExp(`cv-document--${template.templateKey}`))
      for (const text of ['Riga', '+371 20000000', 'Nested description', 'Project lead', 'Contract', 'Remote',
        'Standalone description', 'Jan 2020 - Present']) {
        await expect(preview).toContainText(text)
      }
      for (const text of ['Hidden project', 'Hidden customer', 'hidden@example.com']) {
        await expect(preview).not.toContainText(text)
      }
      if (template.templateKey === 'minimal') await expect(preview.locator('.document-rail')).toContainText('Riga')
      await preview.scrollIntoViewIfNeeded()
      const bounds = await preview.boundingBox()
      expect(bounds!.x).toBeGreaterThanOrEqual(0)
      expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(viewport.width)
      const overflowingText = await preview.locator('h2, h3, h4, h5, p, span, a').evaluateAll((elements) =>
        elements.filter((element) => element.clientWidth > 0 && element.scrollWidth > element.clientWidth + 1)
          .map((element) => element.textContent))
      expect(overflowingText).toEqual([])
      await preview.screenshot({ path: test.info().outputPath(`${template.templateKey}-${viewport.width}.png`) })
    }
  })
}