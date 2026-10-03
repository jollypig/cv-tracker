import { expect, test } from '@playwright/test'

test('redirects a protected workspace visit to the sign-in screen', async ({ page }) => {
  await page.route('**/api/v1/auth/me', (route) =>
    route.fulfill({ status: 401, contentType: 'application/json', body: '{"detail":"Not signed in"}' }),
  )
  await page.route('**/api/v1/auth/config', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ oidcEnabled: false, loginPath: '/oauth2/authorization/oidc' }),
    }),
  )

  await page.goto('/')

  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByRole('heading', { name: 'Sign in to your workspace' })).toBeVisible()
  await expect(page.getByText(/Sign-in is not configured/)).toBeVisible()
})