import { createApp } from 'vue'
import { createPinia } from 'pinia'
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'
import './styles.css'
import { createVuetify } from 'vuetify'
import { aliases, mdi } from 'vuetify/iconsets/mdi'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './shared/stores/authStore'

const pinia = createPinia()

window.addEventListener('cv:unauthorized', () => {
  useAuthStore(pinia).reset()
  void router.replace({ name: 'login' })
})
window.addEventListener('cv:forbidden', () => {
  void router.replace({ name: 'forbidden' })
})

const vuetify = createVuetify({
  icons: { defaultSet: 'mdi', aliases, sets: { mdi } },
  theme: {
    defaultTheme: 'cvLight',
    themes: {
      cvLight: {
        dark: false,
        colors: {
          background: '#f6f5f1',
          surface: '#ffffff',
          primary: '#205c50',
          secondary: '#d5e6dc',
          error: '#b5473c',
        },
      },
    },
  },
})

createApp(App).use(pinia).use(router).use(vuetify).mount('#app')