import { expect, test } from '@playwright/test'
import type { CvContent } from '../src/shared/api/cvTypes'

for (const viewport of [{ width: 1440, height: 900 }, { width: 390, height: 844 }]) {
  test(`merges selected CVs after saving the current draft at ${viewport.width}px`, async ({ page }) => {
    await page.setViewportSize(viewport)
    const target = { id: 'target', personId: 'person', personName: 'Alex Example', name: 'Target CV',
      language: 'en', status: 'DRAFT', tags: [], templateId: null, currentVersionId: null }
    const sources = [{ ...target, id: 'source-one', name: 'Source One' },
      { ...target, id: 'source-two', name: 'Source Two' }]
    const content: CvContent = { summary: 'Original summary', includeSkillDetailsInOutput: false,
      includeSkillLevelsInOutput: true, experiences: [], education: [], skillGroups: [], languages: [],
      projects: [], certifications: [], customSections: [], sections: [] }
    const requests: string[] = []
    let mergeFails = false
    await page.route('**/api/v1/**', async (route) => {
      const path = new URL(route.request().url()).pathname
      let body: unknown = {}
      if (path.endsWith('/auth/me')) body = { id: 'owner', displayName: 'Alex Example' }
      else if (path.endsWith('/auth/config')) body = { oidcEnabled: true }
      else if (path.endsWith('/content/merge')) {
        requests.push('merge')
        expect(route.request().postDataJSON()).toEqual({ sourceCvIds: ['source-one', 'source-two'] })
        if (mergeFails) {
          await route.fulfill({ status: 500, json: { detail: 'Merge failed' } })
          return
        }
        body = { ...content, languages: [{ language: 'German', level: 'Native', reading: null,
          writing: null, speaking: null, sortOrder: 0 }] }
      } else if (path.endsWith('/content')) {
        if (route.request().method() === 'PUT') {
          requests.push('save')
          Object.assign(content, route.request().postDataJSON())
        }
        body = content
      } else if (path.endsWith('/templates')) body = []
      else if (path.endsWith('/persons/person')) body = { id: 'person', firstName: 'Alex', lastName: 'Example', contacts: [] }
      else if (path.endsWith('/cvs/target')) body = target
      else if (path.endsWith('/cvs')) body = [target, ...sources]
      await route.fulfill({ json: body })
    })

    await page.goto('/cvs/target/content')
    await expect(page.getByRole('heading', { name: 'Target CV', exact: true })).toBeVisible()
    await page.getByText('Professional summary', { exact: true }).click()
    await page.getByLabel('Summary', { exact: true }).fill('My current draft')
    await page.getByRole('button', { name: 'More actions' }).click()
    await page.getByText('Merge from other CVs', { exact: true }).click()
    const dialog = page.getByRole('dialog')
    const picker = dialog.getByRole('combobox', { name: 'Source CVs' })
    await expect(dialog.getByRole('button', { name: 'Merge', exact: true })).toBeDisabled()
    await picker.click()
    await expect(page.getByRole('option', { name: /Target CV/ })).toHaveCount(0)
    await page.getByRole('option', { name: /Source One/ }).click()
    await picker.fill('Source Two')
    await page.getByRole('option', { name: /Source Two/ }).click()
    await picker.press('Escape')
    const dialogBounds = await dialog.boundingBox()
    expect(dialogBounds).not.toBeNull()
    expect(dialogBounds!.x).toBeGreaterThanOrEqual(0)
    expect(dialogBounds!.x + dialogBounds!.width).toBeLessThanOrEqual(viewport.width)
    await page.screenshot({ path: test.info().outputPath(`merge-dialog-${viewport.width}.png`) })
    mergeFails = true
    await dialog.getByRole('button', { name: 'Merge', exact: true }).click()
    await expect(dialog.getByText('Unable to merge the selected CVs.')).toBeVisible()
    await expect(dialog).toBeVisible()
    mergeFails = false
    await dialog.getByRole('button', { name: 'Merge', exact: true }).click()
    await expect(dialog).not.toBeVisible()
    await expect(page.getByText('CV changes merged and saved.')).toBeVisible()
    expect(requests).toEqual(['save', 'merge', 'save', 'merge'])
    expect(content.summary).toBe('My current draft')
    await page.getByRole('button', { name: 'Languages 1', exact: true }).click()
    await expect(page.locator('.content-editor').getByLabel('Language', { exact: true })).toHaveValue('German')
  })
}