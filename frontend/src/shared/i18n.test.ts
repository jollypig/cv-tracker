import { afterEach, describe, expect, it, vi } from 'vitest'
import { locale, setLocale, translate } from './i18n'

describe('UI locale', () => {
  afterEach(() => setLocale('en'))

  it('changes translated text and persists the selected locale', () => {
    setLocale('lv')

    expect(translate('navigation.people')).toBe('Personas')
    expect(localStorage.getItem('cv-ui-locale')).toBe('lv')
  })

  it('uses English as the default locale', () => {
    setLocale('en')

    expect(locale.value).toBe('en')
    expect(translate('navigation.people')).toBe('People')
  })

  it('interpolates values in localized messages', () => {
    setLocale('lv')

    expect(translate('people.deleteConfirm', { name: 'Ada Lovelace' }))
      .toBe('Persona Ada Lovelace un tās kontaktinformācija tiks neatgriezeniski dzēsta.')
  })

  it('restores the saved locale when the app initializes', async () => {
    localStorage.setItem('cv-ui-locale', 'lv')
    vi.resetModules()
    const i18n = await import('./i18n')

    expect(i18n.locale.value).toBe('lv')
    expect(i18n.translate('navigation.people')).toBe('Personas')
  })
})