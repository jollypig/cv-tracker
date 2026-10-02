<template>
  <CvTemplateDocument :cv="cv" :person="person" :content="content" variant="minimal" />
</template>

<script setup lang="ts">
import type { Cv, CvContent } from '../../shared/api/cvTypes'
import type { Person } from '../../shared/api/personTypes'
import CvTemplateDocument from './CvTemplateDocument.vue'

defineProps<{ cv: Cv; person: Person; content: CvContent }>()
</script>