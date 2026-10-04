import { createRouter, createWebHistory } from 'vue-router'
import PeopleView from './views/PeopleView.vue'
import PersonFormView from './views/PersonFormView.vue'
import WorkspaceView from './views/WorkspaceView.vue'
import CvFormView from './views/CvFormView.vue'
import CvContentView from './views/CvContentView.vue'
import CvVersionsView from './views/CvVersionsView.vue'
import CvVersionDetailsView from './views/CvVersionDetailsView.vue'
import CvVersionDiffView from './views/CvVersionDiffView.vue'
import AuthView from './views/AuthView.vue'
import { useAuthStore } from './shared/stores/authStore'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: AuthView, meta: { title: 'Sign in', titleKey: 'navigation.signIn', public: true, view: 'login' } },
    { path: '/forbidden', name: 'forbidden', component: AuthView, meta: { title: 'Access denied', titleKey: 'navigation.accessDenied', public: true, view: 'forbidden' } },
    { path: '/', component: PeopleView, meta: { title: 'People', titleKey: 'navigation.peopleTitle' } },
    { path: '/people/new', component: PersonFormView, meta: { title: 'Add person', titleKey: 'navigation.addPerson' } },
    { path: '/people/:id/edit', component: PersonFormView, meta: { title: 'Edit person', titleKey: 'navigation.editPerson' } },
    { path: '/cvs', component: WorkspaceView, meta: { title: 'CV library', titleKey: 'navigation.cvLibrary' } },
    { path: '/cvs/new', component: CvFormView, meta: { title: 'Create CV', titleKey: 'navigation.createCv' } },
    { path: '/cvs/:id/edit', component: CvFormView, meta: { title: 'Edit CV', titleKey: 'navigation.editCv' } },
    { path: '/cvs/:id/content', component: CvContentView, meta: { title: 'Edit CV content', titleKey: 'navigation.editCvContent' } },
    { path: '/cvs/:id/versions', component: CvVersionsView, meta: { title: 'CV versions', titleKey: 'navigation.cvVersions' } },
    { path: '/cvs/:id/versions/diff', component: CvVersionDiffView, meta: { title: 'Compare versions', titleKey: 'navigation.compareVersions' } },
    { path: '/cvs/:id/versions/:versionNumber', component: CvVersionDetailsView, meta: { title: 'Version details', titleKey: 'navigation.versionDetails' } },
    { path: '/people/:personId/cvs', component: WorkspaceView, meta: { title: 'Person CVs', titleKey: 'navigation.personCvs' } },
    { path: '/people/:personId/cvs/new', component: CvFormView, meta: { title: 'Create CV', titleKey: 'navigation.createCv' } },
  ],
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true

  const auth = useAuthStore()
  try {
    if (await auth.load()) {
      if (to.path === '/') {
        const redirect = sessionStorage.getItem('cv-auth-redirect')
        sessionStorage.removeItem('cv-auth-redirect')
        if (redirect && redirect !== '/') return redirect
      }
      return true
    }
  } catch {
    sessionStorage.setItem('cv-auth-redirect', to.fullPath)
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  sessionStorage.setItem('cv-auth-redirect', to.fullPath)
  return { name: 'login', query: { redirect: to.fullPath } }
})

export default router