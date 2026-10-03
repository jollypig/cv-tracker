import apiClient from './axios'

export interface AuthenticatedUser {
  id: string
  email: string | null
  displayName: string | null
}

export interface AuthConfiguration {
  oidcEnabled: boolean
  loginPath: string
}

const authApi = {
  async currentUser(): Promise<AuthenticatedUser> {
    const response = await apiClient.get<AuthenticatedUser>('/auth/me')
    return response.data
  },

  async configuration(): Promise<AuthConfiguration> {
    const response = await apiClient.get<AuthConfiguration>('/auth/config')
    return response.data
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout')
  },
}

export default authApi