<template>
  <main class="cv-content-view">
    <div class="page-heading form-heading">
      <div>
        <div class="eyebrow">{{ translate('editor.eyebrow') }}</div>
        <h1>{{ cv?.name ?? translate('editor.defaultTitle') }}</h1>
        <p>{{ cv?.personName ?? translate('editor.tagline') }}</p>
      </div>
      <div class="content-heading-actions">
        <v-menu v-model="moreActionsOpen" location="bottom end">
          <template #activator="{ props }">
            <v-btn v-bind="moreActionsActivatorProps(props)" variant="text" prepend-icon="mdi-dots-horizontal">{{ translate('editor.moreActions') }}</v-btn>
          </template>
          <v-list>
            <v-list-item :title="translate('editor.share')" prepend-icon="mdi-share-variant" :disabled="loading || !cv" @click="openShareDialog" />
            <v-list-item :title="translate('editor.duplicate')" prepend-icon="mdi-content-copy" @click="openDuplicateDialog" />
            <v-list-item :title="translate('editor.createLanguageVersion')" prepend-icon="mdi-translate" @click="openLanguageVersionDialog" />
          </v-list>
        </v-menu>
        <v-btn variant="text" prepend-icon="mdi-history" :to="`/cvs/${cvId}/versions`">{{ translate('editor.versions') }}</v-btn>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/cvs/${cvId}/edit`">{{ translate('editor.cvDetails') }}</v-btn>
        <v-btn color="primary" :loading="saving" prepend-icon="mdi-content-save-outline" @click="saveContent">{{ translate('editor.save') }}</v-btn>
      </div>
    </div>

    <v-alert v-if="error" class="view-alert" type="error" variant="tonal" closable @click:close="error = ''">
      {{ error }}
    </v-alert>
    <v-alert v-if="saved" class="view-alert" type="success" variant="tonal" closable @click:close="saved = false">
      {{ translate('editor.saved') }}
    </v-alert>
    <v-alert v-if="draftStatus" class="view-alert" type="info" variant="tonal">
      {{ translate(draftStatus) }}
    </v-alert>
    <v-progress-linear v-if="loading" class="form-loading" color="primary" indeterminate />

    <v-dialog v-model="duplicateDialog" max-width="480">
      <v-card>
        <v-card-title class="dialog-title">{{ translate(duplicateAsLanguageVersion ? 'editor.languageVersionTitle' : 'editor.duplicateTitle') }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="duplicateName" :label="translate('editor.copyName')" maxlength="255" variant="outlined" autofocus />
          <v-text-field
            v-if="duplicateAsLanguageVersion"
            v-model="duplicateLanguage"
            :label="translate('editor.cvLanguageCode')"
            maxlength="10"
            variant="outlined"
            required
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="duplicateDialog = false">{{ translate('editor.cancel') }}</v-btn>
          <v-btn color="primary" :loading="duplicating" :disabled="!duplicateName.trim() || (duplicateAsLanguageVersion && !duplicateLanguage.trim())" @click="duplicateCv">
            {{ translate(duplicateAsLanguageVersion ? 'editor.createLanguageVersionAction' : 'editor.createCopy') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="shareDialog" max-width="520">
      <v-card>
        <v-card-title class="dialog-title">{{ translate('editor.shareTitle') }}</v-card-title>
        <v-card-text>
          <p>{{ translate('editor.shareDescription') }}</p>
          <v-alert v-if="shareEnabled" class="share-status" type="success" variant="tonal">
            {{ translate('editor.shareEnabled') }}
          </v-alert>
          <div v-if="shareEnabled" class="share-analytics">
            <p>{{ translate('editor.shareViewCount', { count: String(shareViewCount) }) }}</p>
            <p>{{ translate('editor.shareLastViewed') }}: {{ formattedShareLastViewed }}</p>
          </div>
          <v-text-field v-if="shareUrl" :model-value="shareUrl" :label="translate('editor.shareLink')" readonly variant="outlined">
            <template #append-inner>
              <v-btn icon="mdi-content-copy" size="small" variant="text" :aria-label="translate('editor.copyShareLink')" :title="translate('editor.copyShareLink')" @click="copyShareLink" />
            </template>
          </v-text-field>
          <v-alert v-if="shareCopied" type="success" variant="tonal">{{ translate('editor.shareCopied') }}</v-alert>
          <v-alert v-if="shareError" type="error" variant="tonal">{{ translate(shareError) }}</v-alert>
        </v-card-text>
        <v-card-actions>
          <v-btn v-if="shareEnabled" color="error" variant="text" :loading="shareLoading" @click="revokeShareLink">
            {{ translate('editor.revokeShareLink') }}
          </v-btn>
          <v-spacer />
          <v-btn variant="text" @click="shareDialog = false">{{ translate('editor.cancel') }}</v-btn>
          <v-btn color="primary" :loading="shareLoading" @click="createShareLink">
            {{ translate(shareEnabled ? 'editor.replaceShareLink' : 'editor.createShareLink') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <div v-if="!loading && cv && person" class="content-workspace">
    <v-form ref="editorForm" class="content-editor" @submit.prevent="saveContent">
      <section class="content-order-section">
        <div class="form-section-heading">
          <h2>{{ translate('editor.orderTitle') }}</h2>
          <p>{{ translate('editor.orderHelp') }}</p>
        </div>
        <div class="content-section-order">
          <div
            v-for="(section, index) in content.sections"
            :key="section.type"
            class="content-order-row"
            draggable="true"
            @dragstart="draggedSection = index"
            @dragover.prevent
            @drop="dropSection(index)"
            @dragend="draggedSection = null"
          >
            <v-icon icon="mdi-drag" class="drag-handle" aria-hidden="true" />
            <span class="order-number">{{ index + 1 }}</span>
            <span class="order-label">{{ sectionTitle(section.type) }}</span>
            <v-switch v-model="section.visible" :aria-label="translate('editor.showSection', { section: sectionTitle(section.type) })" color="primary" density="compact" hide-details />
            <v-btn :disabled="index === 0" :aria-label="translate('editor.moveSectionUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveSection(index, -1)" />
            <v-btn :disabled="index === content.sections.length - 1" :aria-label="translate('editor.moveSectionDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveSection(index, 1)" />
          </div>
        </div>
      </section>

      <v-expansion-panels multiple class="content-panels">
        <v-expansion-panel value="summary">
          <v-expansion-panel-title>{{ translate('editor.summary') }}</v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-textarea v-model="content.summary" :label="translate('editor.summaryField')" rows="5" counter="10000" variant="outlined" />
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="experience">
          <v-expansion-panel-title>{{ translate('editor.experience') }} <span class="panel-count">{{ content.experiences.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(experience, index) in content.experiences" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>{{ translate('editor.positionNumber', { number: index + 1 }) }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" :aria-label="translate('editor.moveExperienceUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.experiences, index, -1)" />
                <v-btn :disabled="index === content.experiences.length - 1" :aria-label="translate('editor.moveExperienceDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.experiences, index, 1)" />
                <v-btn :aria-label="translate('editor.removePosition', { number: index + 1 })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.experiences, index)" />
              </div></div>
              <div class="content-field-grid">
                <v-text-field v-model="experience.company" :label="translate('editor.company')" variant="outlined" :rules="[requiredRule]" />
                <v-text-field v-model="experience.position" :label="translate('editor.position')" variant="outlined" :rules="[requiredRule]" />
                <v-text-field v-model="experience.location" :label="translate('editor.location')" variant="outlined" />
                <v-select v-model="experience.employmentType" :items="employmentTypes" item-title="title" item-value="value" :label="translate('editor.employmentType')" variant="outlined" clearable />
                <v-select v-model="experience.employmentLocation" :items="employmentLocations" item-title="title" item-value="value" :label="translate('editor.employmentLocation')" variant="outlined" clearable />
                <v-text-field v-model="experience.startDate" :label="translate('editor.startDate')" type="date" variant="outlined" />
                <v-text-field v-model="experience.endDate" :label="translate('editor.endDate')" type="date" variant="outlined" :rules="[() => dateOrderRule(experience.startDate, experience.endDate)]" />
                <v-checkbox v-model="experience.current" :label="translate('editor.currentWork')" hide-details />
              </div>
              <v-textarea v-model="experience.description" :label="translate('editor.description')" rows="3" variant="outlined" />
              <div class="nested-editor">
                <div class="entry-heading"><h4>{{ translate('editor.projectsAtPosition') }}</h4><v-btn size="small" prepend-icon="mdi-plus" variant="text" @click="addExperienceProject(experience)">{{ translate('editor.addProject') }}</v-btn></div>
                <div v-for="(project, projectIndex) in experience.projects" :key="projectIndex" class="nested-entry">
                  <div class="entry-heading"><span>{{ translate('editor.projectNumber', { number: projectIndex + 1 }) }}</span><v-btn :aria-label="translate('editor.removeProject', { number: projectIndex + 1 })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(experience.projects, projectIndex)" /></div>
                  <div class="content-field-grid">
                    <v-checkbox
                        :model-value="project.showProjectName === false && project.showCustomerCompany === false"
                        class="project-visibility-toggle"
                        :label="translate('editor.hideProjectNameCompany')"
                        hide-details
                        @update:model-value="setProjectNameAndCompanyHidden(project, $event)"
                    />
                    <v-text-field v-model="project.projectName" :label="translate('editor.projectName')" variant="outlined" :rules="[requiredRule]" />
                    <v-text-field v-model="project.company" :label="translate('editor.customerCompany')" variant="outlined" />
                    <v-textarea v-model="project.projectDescription" class="project-description-field" :label="translate('editor.projectDescription')" rows="2" variant="outlined" />
                    <v-text-field v-model="project.industries" :label="translate('editor.industries')" variant="outlined" />
                    <v-text-field v-model="project.position" :label="translate('editor.projectPosition')" variant="outlined" />
                    <v-text-field v-model="project.periodFrom" :label="translate('editor.from')" type="date" variant="outlined" />
                    <v-text-field v-model="project.periodTo" :label="translate('editor.to')" type="date" variant="outlined" :rules="[() => dateOrderRule(project.periodFrom, project.periodTo)]" />
                    <v-text-field v-model.number="project.teamSize" :label="translate('editor.teamSize')" type="number" min="0" variant="outlined" />
                    <v-text-field v-model="project.externalLink" :label="translate('editor.externalLink')" type="url" variant="outlined" :rules="[optionalUrlRule]" />
                  </div>
                  <v-textarea v-model="project.responsibilities" :label="translate('editor.responsibilities')" rows="2" variant="outlined" />
                  <v-textarea v-model="project.technologies" :label="translate('editor.technologies')" rows="2" variant="outlined" />
                  <v-autocomplete
                    :model-value="linkedSkillIds(project)"
                    :items="projectSkillOptions"
                    item-title="title"
                    item-value="value"
                    :label="translate('editor.linkedSkills')"
                    variant="outlined"
                    density="compact"
                    multiple
                    chips
                    closable-chips
                    clearable
                    :no-data-text="translate('editor.addSkillsFirst')"
                    @update:model-value="updateProjectSkills(project, $event)"
                  >
                    <template #item="{ props: itemProps, item }">
                      <v-list-item v-bind="itemProps" :subtitle="item.raw.group" />
                    </template>
                  </v-autocomplete>
                </div>
              </div>
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addExperience">{{ translate('editor.addExperience') }}</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="education">
          <v-expansion-panel-title>{{ translate('editor.education') }} <span class="panel-count">{{ content.education.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(education, index) in content.education" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>{{ translate('editor.educationNumber', { number: index + 1 }) }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" :aria-label="translate('editor.moveEducationUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.education, index, -1)" />
                <v-btn :disabled="index === content.education.length - 1" :aria-label="translate('editor.moveEducationDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.education, index, 1)" />
                <v-btn :aria-label="translate('editor.removeEducation', { number: index + 1 })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.education, index)" />
              </div></div>
              <div class="content-field-grid">
                <v-text-field v-model="education.institution" :label="translate('editor.institution')" variant="outlined" :rules="[requiredRule]" />
                <v-select v-model="education.degree" :items="degreeOptions" item-title="title" item-value="value" :label="translate('editor.degree')" clearable variant="outlined" />
                <v-text-field v-model="education.fieldOfStudy" :label="translate('editor.fieldOfStudy')" variant="outlined" />
                <v-text-field v-model="education.startDate" :label="translate('editor.startDate')" type="date" variant="outlined" />
                <v-text-field v-model="education.endDate" :label="translate('editor.endDate')" type="date" variant="outlined" :rules="[() => dateOrderRule(education.startDate, education.endDate)]" />
                <v-checkbox v-model="education.current" :label="translate('editor.currentStudy')" hide-details />
              </div>
              <v-textarea v-model="education.diplomaDegreeWork" :label="translate('editor.diplomaWork')" rows="2" variant="outlined" />
              <v-textarea v-model="education.description" :label="translate('editor.description')" rows="2" variant="outlined" />
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addEducation">{{ translate('editor.addEducation') }}</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="skills">
          <v-expansion-panel-title>{{ translate('editor.skillGroups') }} <span class="panel-count">{{ content.skillGroups.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-expansion-panels v-model="expandedSkillGroups" multiple class="skill-group-panels">
              <v-expansion-panel v-for="(group, groupIndex) in content.skillGroups" :key="groupIndex" :value="groupIndex">
                <v-expansion-panel-title>
                  <span>{{ group.name || translate('editor.skillGroupNumber', { number: groupIndex + 1 }) }}</span>
                  <span class="panel-count">{{ group.skills.length }}</span>
                  <template #actions>
                    <div class="entry-actions" @click.stop>
                      <v-btn :disabled="groupIndex === 0" :aria-label="translate('editor.moveSkillGroupUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.skillGroups, groupIndex, -1)" />
                      <v-btn :disabled="groupIndex === content.skillGroups.length - 1" :aria-label="translate('editor.moveSkillGroupDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.skillGroups, groupIndex, 1)" />
                      <v-btn :aria-label="translate('editor.removeSkillGroup', { name: group.name || translate('editor.skillGroups') })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.skillGroups, groupIndex)" />
                      <v-icon class="skill-group-expand-icon" icon="mdi-chevron-down" aria-hidden="true" />
                    </div>
                  </template>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <v-text-field v-model="group.name" :label="translate('editor.groupName')" variant="outlined" :rules="[requiredRule]" />
                  <div v-for="(skill, skillIndex) in group.skills" :key="skillIndex" class="skill-item">
                    <div class="content-field-grid skill-entry">
                      <v-text-field v-model="skill.name" :label="translate('editor.skill')" variant="outlined" :rules="[requiredRule]" />
                      <v-select v-model="skill.level" :items="skillLevels" item-title="title" item-value="value" :label="translate('editor.level')" variant="outlined" />
                      <div class="entry-actions">
                        <v-btn :disabled="skillIndex === 0" :aria-label="translate('editor.moveSkillUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(group.skills, skillIndex, -1)" />
                        <v-btn :disabled="skillIndex === group.skills.length - 1" :aria-label="translate('editor.moveSkillDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(group.skills, skillIndex, 1)" />
                        <v-btn :aria-label="translate('editor.removeSkill', { number: skillIndex + 1 })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(group.skills, skillIndex)" />
                      </div>
                    </div>
                    <v-checkbox v-model="skill.visible" class="skill-visibility" density="compact" hide-details :label="translate('editor.includeOutput')" />
                    <SkillDetailsEditor :skill="skill" :content="content" />
                  </div>
                  <v-btn size="small" prepend-icon="mdi-plus" variant="text" @click="addSkill(group)">{{ translate('editor.addSkill') }}</v-btn>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addSkillGroup">{{ translate('editor.addSkillGroup') }}</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="languages">
          <v-expansion-panel-title>{{ translate('editor.languages') }} <span class="panel-count">{{ content.languages.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(language, index) in content.languages" :key="index" class="language-entry">
              <div class="content-field-grid list-entry">
                <v-text-field v-model="language.language" :label="translate('editor.language')" variant="outlined" :rules="[requiredRule]" />
                <v-select v-model="language.level" :items="languageLevels" item-title="title" item-value="value" :label="translate('editor.proficiency')" variant="outlined" />
                <div class="entry-actions">
                  <v-btn :disabled="index === 0" :aria-label="translate('editor.moveLanguageUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.languages, index, -1)" />
                  <v-btn :disabled="index === content.languages.length - 1" :aria-label="translate('editor.moveLanguageDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.languages, index, 1)" />
                  <v-btn :aria-label="translate('editor.removeLanguage', { number: index + 1 })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.languages, index)" />
                </div>
              </div>
              <v-checkbox :model-value="hasDetailedLevels(language)" density="compact" hide-details :label="translate('editor.detailedLevels')" @update:model-value="setDetailedLevels(language, $event)" />
              <div v-if="hasDetailedLevels(language)" class="language-detail-grid">
                <v-select v-model="language.reading" :items="languageLevels" item-title="title" item-value="value" :label="translate('editor.reading')" variant="outlined" />
                <v-select v-model="language.writing" :items="languageLevels" item-title="title" item-value="value" :label="translate('editor.writing')" variant="outlined" />
                <v-select v-model="language.speaking" :items="languageLevels" item-title="title" item-value="value" :label="translate('editor.speaking')" variant="outlined" />
              </div>
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addLanguage">{{ translate('editor.addLanguage') }}</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="projects">
          <v-expansion-panel-title>{{ translate('editor.projects') }} <span class="panel-count">{{ content.projects.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(project, index) in content.projects" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>{{ project.name || translate('editor.projectNumber', { number: index + 1 }) }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" :aria-label="translate('editor.moveProjectUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.projects, index, -1)" />
                <v-btn :disabled="index === content.projects.length - 1" :aria-label="translate('editor.moveProjectDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.projects, index, 1)" />
                <v-btn :aria-label="translate('editor.removeProject', { number: index + 1 })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.projects, index)" />
              </div></div>
              <div class="content-field-grid">
                <v-text-field v-model="project.name" :label="translate('editor.projectName')" variant="outlined" :rules="[requiredRule]" />
                <v-text-field v-model="project.role" :label="translate('editor.projectRole')" variant="outlined" />
                <v-text-field v-model="project.technologies" :label="translate('editor.technologiesShort')" variant="outlined" />
                <v-text-field v-model="project.url" :label="translate('editor.projectUrl')" type="url" variant="outlined" :rules="[optionalUrlRule]" />
                <v-text-field v-model="project.periodFrom" :label="translate('editor.periodFrom')" type="date" variant="outlined" />
                <v-text-field v-model="project.periodTo" :label="translate('editor.periodTo')" type="date" :disabled="project.current" variant="outlined" :rules="[() => dateOrderRule(project.periodFrom ?? null, project.periodTo ?? null)]" />
              </div>
              <v-checkbox v-model="project.current" :label="translate('editor.currentProject')" density="compact" hide-details @update:model-value="project.periodTo = null" />
              <v-textarea v-model="project.description" :label="translate('editor.description')" rows="2" variant="outlined" />
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addProject">{{ translate('editor.addProject') }}</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="certifications">
          <v-expansion-panel-title>{{ translate('editor.certifications') }} <span class="panel-count">{{ content.certifications.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(certification, index) in content.certifications" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>{{ certification.name || translate('editor.certificationNumber', { number: index + 1 }) }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" :aria-label="translate('editor.moveCertificationUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.certifications, index, -1)" />
                <v-btn :disabled="index === content.certifications.length - 1" :aria-label="translate('editor.moveCertificationDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.certifications, index, 1)" />
                <v-btn :aria-label="translate('editor.removeCertification', { number: index + 1 })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.certifications, index)" />
              </div></div>
              <div class="content-field-grid">
                <v-text-field v-model="certification.name" :label="translate('editor.certification')" variant="outlined" :rules="[requiredRule]" />
                <v-text-field v-model="certification.issuer" :label="translate('editor.issuer')" variant="outlined" />
                <v-text-field v-model="certification.issueDate" :label="translate('editor.issueDate')" type="date" variant="outlined" />
                <v-text-field v-model="certification.expiryDate" :label="translate('editor.expiryDate')" type="date" variant="outlined" :rules="[() => dateOrderRule(certification.issueDate, certification.expiryDate)]" />
                <v-text-field v-model="certification.credentialId" :label="translate('editor.credentialId')" variant="outlined" />
                <v-text-field v-model="certification.credentialUrl" :label="translate('editor.credentialUrl')" type="url" variant="outlined" :rules="[optionalUrlRule]" />
              </div>
              <v-textarea v-model="certification.description" :label="translate('editor.description')" variant="outlined" rows="3" auto-grow />
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addCertification">{{ translate('editor.addCertification') }}</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel value="custom">
          <v-expansion-panel-title>{{ translate('editor.customSections') }} <span class="panel-count">{{ content.customSections.length }}</span></v-expansion-panel-title>
          <v-expansion-panel-text>
            <div v-for="(section, index) in content.customSections" :key="index" class="editor-entry">
              <div class="entry-heading"><h3>{{ section.title || translate('editor.customSectionNumber', { number: index + 1 }) }}</h3><div class="entry-actions">
                <v-btn :disabled="index === 0" :aria-label="translate('editor.moveCustomSectionUp')" icon="mdi-arrow-up" size="small" variant="text" @click="moveItem(content.customSections, index, -1)" />
                <v-btn :disabled="index === content.customSections.length - 1" :aria-label="translate('editor.moveCustomSectionDown')" icon="mdi-arrow-down" size="small" variant="text" @click="moveItem(content.customSections, index, 1)" />
                <v-btn :aria-label="translate('editor.removeCustomSection', { number: index + 1 })" color="error" icon="mdi-delete-outline" size="small" variant="text" @click="removeItem(content.customSections, index)" />
              </div></div>
              <v-text-field v-model="section.title" :label="translate('editor.sectionTitle')" variant="outlined" :rules="[requiredRule]" />
              <v-textarea v-model="section.content" :label="translate('editor.content')" rows="4" variant="outlined" />
            </div>
            <v-btn prepend-icon="mdi-plus" variant="tonal" @click="addCustomSection">{{ translate('editor.addCustomSection') }}</v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>

      <div class="form-actions content-save-actions">
        <v-btn variant="text" :to="`/cvs/${cvId}/edit`">{{ translate('editor.cancel') }}</v-btn>
        <v-btn color="primary" type="submit" :loading="saving" prepend-icon="mdi-content-save-outline">{{ translate('editor.save') }}</v-btn>
      </div>
    </v-form>
    <CvPreview
      :cv="cv"
      :person="person"
      :content="content"
      :templates="templates"
      :selected-template-id="selectedTemplateId"
      :disabled="selectingTemplate"
      @select-template="selectTemplate"
    />
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CvPreview from '../components/templates/CvPreview.vue'
import SkillDetailsEditor from '../components/SkillDetailsEditor.vue'
import { emptySkillDetails } from '../shared/skillMetrics'
import cvApi from '../shared/api/cvApi'
import personApi from '../shared/api/personApi'
import type {
  Cv, CvCertification, CvContent, CvCustomSection, CvEducation, CvExperience, CvExperienceProject, CvTemplate,
  CvLanguage, CvProject, CvSection, CvSectionType, CvSkill, CvSkillGroup,
} from '../shared/api/cvTypes'
import type { Person } from '../shared/api/personTypes'
import { translate } from '../shared/i18n'

const route = useRoute()
const router = useRouter()
const cvId = typeof route.params.id === 'string' ? route.params.id : ''
const cv = ref<Cv | null>(null)
const person = ref<Person | null>(null)
const templates = ref<CvTemplate[]>([])
const selectedTemplateId = ref('')
const loading = ref(true)
const saving = ref(false)
const selectingTemplate = ref(false)
const error = ref('')
const saved = ref(false)
const moreActionsOpen = ref(false)
const duplicateDialog = ref(false)
const shareDialog = ref(false)
const shareEnabled = ref(false)
const shareViewCount = ref(0)
const shareLastViewedAt = ref<string | null>(null)
const shareUrl = ref('')
const shareLoading = ref(false)
const shareError = ref('')
const shareCopied = ref(false)

function moreActionsActivatorProps(props: Record<string, unknown>) {
  return {
    ...props,
    onClick: () => {
      moreActionsOpen.value = !moreActionsOpen.value
    },
  }
}

const duplicateName = ref('')
const duplicateLanguage = ref('')
const duplicateAsLanguageVersion = ref(false)
const duplicating = ref(false)
const draftStatus = ref('')
const draftTracking = ref(false)
let draftTimer: ReturnType<typeof setTimeout> | undefined
const draggedSection = ref<number | null>(null)
const editorForm = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const sectionDefinitions: Array<{ type: CvSectionType }> = [
  { type: 'SUMMARY' },
  { type: 'EXPERIENCE' },
  { type: 'EDUCATION' },
  { type: 'SKILLS' },
  { type: 'LANGUAGES' },
  { type: 'PROJECTS' },
  { type: 'CERTIFICATIONS' },
  { type: 'CUSTOM' },
]
const sectionTitleKeys: Record<CvSectionType, string> = {
  SUMMARY: 'editor.summary', EXPERIENCE: 'editor.experience', EDUCATION: 'editor.education', SKILLS: 'editor.skills',
  LANGUAGES: 'editor.languages', PROJECTS: 'editor.projects', CERTIFICATIONS: 'editor.certifications', CUSTOM: 'editor.customSections',
}
const languageLevels = computed(() => [
  ['editor.native', 'Native (C2)'], ['editor.fluent', 'Fluent (C1)'], ['editor.advanced', 'Advanced (B2)'],
  ['editor.intermediate', 'Intermediate (B1)'], ['editor.basic', 'Basic (A1–A2)'],
].map(([key, value]) => ({ title: translate(key!), value: value! })))
const skillLevels = computed(() => [
  ['editor.skillBasic', 'Basic'], ['editor.skillIntermediate', 'Intermediate'],
  ['editor.skillAdvanced', 'Advanced'], ['editor.skillExpert', 'Expert'],
].map(([key, value]) => ({ title: translate(key!), value: value! })))
const employmentTypes = computed(() => [
  ['editor.fullTime', 'Full-time'], ['editor.partTime', 'Part-time'], ['editor.contract', 'Contract'],
  ['editor.freelance', 'Freelance'], ['editor.internship', 'Internship'], ['editor.selfEmployed', 'Self-employed'],
].map(([key, value]) => ({ title: translate(key!), value: value! })))
const employmentLocations = computed(() => [
  ['editor.onSite', 'On-site'], ['editor.hybrid', 'Hybrid'], ['editor.remote', 'Remote'],
].map(([key, value]) => ({ title: translate(key!), value: value! })))
const degreeOptions = computed(() => [
  ['editor.highSchool', 'High School'], ['editor.professional', 'Professional'], ['editor.associate', 'Associate Degree'],
  ['editor.bachelor', 'Bachelor’s Degree'], ['editor.master', 'Master’s Degree'], ['editor.doctorate', 'Doctorate (PhD)'],
].map(([key, value]) => ({ title: translate(key!), value: value! })))
const detailedLanguages = reactive(new Set<CvLanguage>())
const expandedSkillGroups = ref<number[]>([])
const content = reactive<CvContent>(emptyContent())
const projectSkillOptions = computed(() => content.skillGroups.flatMap((group, groupIndex) =>
  group.skills.flatMap((skill, skillIndex) => skill.name.trim() ? [{
    value: `${groupIndex}:${skillIndex}`,
    title: skill.name,
    group: group.name || translate('editor.ungrouped'),
  }] : [])))
const formattedShareLastViewed = computed(() => shareLastViewedAt.value
  ? new Date(shareLastViewedAt.value).toLocaleString()
  : translate('editor.shareNeverViewed'))

onMounted(async () => {
  try {
    const [cvData, contentData, templateData] = await Promise.all([
      cvApi.get(cvId), cvApi.getContent(cvId), cvApi.listTemplates(),
    ])
    const personData = await personApi.get(cvData.personId)
    cv.value = cvData
    person.value = personData
    templates.value = templateData
    selectedTemplateId.value = cvData.templateId ?? ''
    Object.assign(content, contentData)
    content.sections.sort((left, right) => left.sortOrder - right.sortOrder)
    restoreLocalDraft()
    initializeSkillDetails()
    draftTracking.value = true
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('editor.loadError')
  } finally {
    loading.value = false
  }
})

watch(content, scheduleLocalDraft, { deep: true })

function openDuplicateDialog() {
  duplicateAsLanguageVersion.value = false
  duplicateName.value = `${cv.value?.name ?? 'CV'} (Copy)`
  duplicateDialog.value = true
}

async function openShareDialog() {
  if (!await saveContent()) return
  shareDialog.value = true
  shareLoading.value = true
  shareError.value = ''
  try {
    const status = await cvApi.shareStatus(cvId)
    shareEnabled.value = status.enabled
    shareViewCount.value = status.viewCount
    shareLastViewedAt.value = status.lastViewedAt
  } catch {
    shareError.value = 'editor.shareLoadError'
  } finally {
    shareLoading.value = false
  }
}

async function createShareLink() {
  shareLoading.value = true
  shareError.value = ''
  shareCopied.value = false
  try {
    const link = await cvApi.createShareLink(cvId)
    shareEnabled.value = true
    shareUrl.value = link.url
  } catch {
    shareError.value = 'editor.shareCreateError'
  } finally {
    shareLoading.value = false
  }
}

async function revokeShareLink() {
  shareLoading.value = true
  shareError.value = ''
  shareCopied.value = false
  try {
    await cvApi.revokeShareLink(cvId)
    shareEnabled.value = false
    shareViewCount.value = 0
    shareLastViewedAt.value = null
    shareUrl.value = ''
  } catch {
    shareError.value = 'editor.shareRevokeError'
  } finally {
    shareLoading.value = false
  }
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    shareCopied.value = true
    shareError.value = ''
  } catch {
    shareCopied.value = false
    shareError.value = 'editor.shareClipboardError'
  }
}

function openLanguageVersionDialog() {
  duplicateAsLanguageVersion.value = true
  duplicateName.value = cv.value?.name ?? 'CV'
  duplicateLanguage.value = ''
  duplicateDialog.value = true
}

async function duplicateCv() {
  if (!duplicateName.value.trim() || (duplicateAsLanguageVersion.value && !duplicateLanguage.value.trim())) return
  duplicating.value = true
  error.value = ''
  try {
    const copy = await cvApi.duplicate(cvId, duplicateName.value.trim(), undefined,
      duplicateAsLanguageVersion.value ? duplicateLanguage.value.trim() : undefined)
    await router.push(`/cvs/${copy.id}/content`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('editor.duplicateError')
  } finally {
    duplicating.value = false
    duplicateDialog.value = false
  }
}

onBeforeUnmount(() => {
  if (draftTimer) clearTimeout(draftTimer)
  saveLocalDraft()
})

function emptyContent(): CvContent {
  return {
    summary: '', experiences: [], education: [], skillGroups: [], languages: [], projects: [],
    certifications: [], customSections: [],
    sections: sectionDefinitions.map((section, sortOrder) => ({ type: section.type, visible: true, sortOrder })),
  }
}

function sectionTitle(type: CvSectionType) {
  return translate(sectionTitleKeys[type])
}

async function selectTemplate(templateId: string) {
  if (!cv.value || !templateId || templateId === selectedTemplateId.value) return
  selectingTemplate.value = true
  error.value = ''
  try {
    cv.value = await cvApi.selectTemplate(cvId, templateId)
    selectedTemplateId.value = templateId
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('editor.templateError')
  } finally {
    selectingTemplate.value = false
  }
}

function requiredRule(value: string | null) {
  return Boolean(value?.trim()) || translate('editor.required')
}

function dateOrderRule(start: string | null, end: string | null) {
  return !start || !end || end >= start || translate('editor.dateOrder')
}

function optionalUrlRule(value: string | null) {
  if (!value?.trim()) return true
  try {
    const protocol = new URL(value).protocol
    return (protocol === 'http:' || protocol === 'https:') || translate('editor.invalidUrl')
  } catch {
    return translate('editor.invalidUrl')
  }
}

function normalizeOrder<T extends { sortOrder: number }>(items: T[]) {
  items.forEach((item, index) => { item.sortOrder = index })
}

function moveItem<T extends { sortOrder: number }>(items: T[], index: number, direction: number) {
  const target = index + direction
  if (target < 0 || target >= items.length) return
  ;[items[index], items[target]] = [items[target]!, items[index]!]
  normalizeOrder(items)
}

function removeItem<T extends { sortOrder: number }>(items: T[], index: number) {
  items.splice(index, 1)
  normalizeOrder(items)
}

function moveSection(index: number, direction: number) {
  moveItem(content.sections, index, direction)
}

function dropSection(target: number) {
  const source = draggedSection.value
  if (source === null || source === target) return
  const [section] = content.sections.splice(source, 1)
  if (section) content.sections.splice(target, 0, section)
  normalizeOrder(content.sections)
  draggedSection.value = null
}

function addExperience() {
  content.experiences.push({ company: '', position: '', location: '', employmentType: null, employmentLocation: null, startDate: null, endDate: null,
    current: false, description: '', sortOrder: content.experiences.length, projects: [] })
}

function addExperienceProject(experience: CvExperience) {
  experience.projects.push({ projectKey: crypto.randomUUID(), company: '', industries: '', projectName: '', projectDescription: '', showProjectName: true, showCustomerCompany: true,
    periodFrom: null, periodTo: null, position: '', responsibilities: '', technologies: '',
    teamSize: null, externalLink: '', sortOrder: experience.projects.length })
}

function linkedSkillIds(project: CvExperienceProject) {
  if (!project.projectKey) return []
  return content.skillGroups.flatMap((group, groupIndex) => group.skills.flatMap((skill, skillIndex) =>
    skill.name.trim() && skill.details?.linkedProjects.some((link) => link.projectKey === project.projectKey)
      ? [`${groupIndex}:${skillIndex}`] : []))
}

function updateProjectSkills(project: CvExperienceProject, selectedIds: string[] | null) {
  if (!project.projectKey) return
  const selected = new Set(selectedIds ?? [])
  content.skillGroups.forEach((group, groupIndex) => group.skills.forEach((skill, skillIndex) => {
    if (!skill.name.trim()) return
    const key = `${groupIndex}:${skillIndex}`
    const details = skill.details ?? emptySkillDetails()
    const linkedProjects = details.linkedProjects ?? []
    const isLinked = linkedProjects.some((link) => link.projectKey === project.projectKey)
    const shouldLink = selected.has(key)
    if (shouldLink && !isLinked) {
      skill.details = { ...details, linkedProjects: [...linkedProjects, { projectKey: project.projectKey!, outcome: '' }] }
    } else if (!shouldLink && isLinked) {
      skill.details = { ...details, linkedProjects: linkedProjects.filter((link) => link.projectKey !== project.projectKey) }
    }
  }))
}

function setProjectNameAndCompanyHidden(project: CvExperienceProject, hidden: boolean | null) {
  project.showProjectName = !hidden
  project.showCustomerCompany = !hidden
}

function addEducation() {
  content.education.push({ institution: '', degree: '', diplomaDegreeWork: '', fieldOfStudy: '', startDate: null, endDate: null, current: false,
    description: '', sortOrder: content.education.length })
}

function addSkillGroup() {
  const groupIndex = content.skillGroups.length
  content.skillGroups.push({ name: '', sortOrder: content.skillGroups.length, skills: [] })
  expandedSkillGroups.value = [...expandedSkillGroups.value, groupIndex]
}

function addSkill(group: CvSkillGroup) {
  group.skills.push({ name: '', level: '', sortOrder: group.skills.length, visible: true, details: emptySkillDetails() })
}

function initializeSkillDetails() {
  content.experiences.forEach((experience) => experience.projects.forEach((project) => {
    project.projectKey ||= crypto.randomUUID()
  }))
  content.projects.forEach((project) => { project.projectKey ||= crypto.randomUUID() })
  content.skillGroups.forEach((group) => group.skills.forEach((skill) => {
    skill.details = { ...emptySkillDetails(), ...skill.details, linkedProjects: skill.details?.linkedProjects ?? [] }
  }))
}

function addLanguage() {
  content.languages.push({ language: '', level: '', reading: '', writing: '', speaking: '', sortOrder: content.languages.length })
}

function hasDetailedLevels(language: CvLanguage) {
  return detailedLanguages.has(language) || [language.reading, language.writing, language.speaking]
    .some((level) => Boolean(level?.trim()))
}

function setDetailedLevels(language: CvLanguage, enabled: boolean | null) {
  if (enabled) {
    detailedLanguages.add(language)
    return
  }
  detailedLanguages.delete(language)
  language.reading = ''
  language.writing = ''
  language.speaking = ''
}

function addProject() {
  content.projects.push({ projectKey: crypto.randomUUID(), name: '', role: '', description: '', technologies: '', url: '',
    periodFrom: null, periodTo: null, current: false, sortOrder: content.projects.length })
}

function addCertification() {
  content.certifications.push({ name: '', description: '', issuer: '', issueDate: null, expiryDate: null,
    credentialId: '', credentialUrl: '', sortOrder: content.certifications.length })
}

function addCustomSection() {
  content.customSections.push({ title: '', content: '', sortOrder: content.customSections.length })
}

async function saveContent(): Promise<boolean> {
  error.value = ''
  saved.value = false
  if (draftTimer) clearTimeout(draftTimer)
  saveLocalDraft()
  const validation = await editorForm.value?.validate()
  if (validation && !validation.valid) return false
  saving.value = true
  try {
    content.sections.forEach((section, index) => { section.sortOrder = index })
    const savedContent = await cvApi.saveContent(cvId, content)
    draftTracking.value = false
    Object.assign(content, savedContent)
    initializeSkillDetails()
    await nextTick()
    try {
      localStorage.removeItem(draftStorageKey())
      draftStatus.value = ''
    } catch {
      draftStatus.value = 'editor.unsavedClearError'
    }
    draftTracking.value = true
    saved.value = true
    return true
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : translate('editor.saveError')
    return false
  } finally {
    saving.value = false
  }
}

function draftStorageKey() {
  return `cv-content-draft:${cvId}`
}

function restoreLocalDraft() {
  try {
    const serialized = localStorage.getItem(draftStorageKey())
    if (!serialized) return
    const draft: unknown = JSON.parse(serialized)
    if (!isContentDraft(draft)) {
      localStorage.removeItem(draftStorageKey())
      return
    }
    Object.assign(content, draft)
    draftStatus.value = 'editor.recoveredDraft'
  } catch {
    draftStatus.value = 'editor.unreadableDraft'
  }
}

function isContentDraft(value: unknown): value is CvContent {
  if (typeof value !== 'object' || value === null) return false
  const draft = value as Record<string, unknown>
  return ['experiences', 'education', 'skillGroups', 'languages', 'projects', 'certifications', 'customSections', 'sections']
    .every((field) => Array.isArray(draft[field]))
}

function scheduleLocalDraft() {
  if (!draftTracking.value) return
  draftStatus.value = 'editor.saveDraftStatus'
  if (draftTimer) clearTimeout(draftTimer)
  draftTimer = setTimeout(saveLocalDraft, 700)
}

function saveLocalDraft() {
  if (!draftTracking.value) return
  try {
    localStorage.setItem(draftStorageKey(), JSON.stringify(content))
    draftStatus.value = 'editor.autosavedStatus'
  } catch {
    draftStatus.value = 'editor.autosaveError'
  }
}
</script>

<style scoped>
.cv-content-view { max-width: 1440px; margin: 0 auto; }
.content-workspace { display: grid; grid-template-columns: minmax(0, 1fr) minmax(360px, 42%); align-items: start; gap: 30px; }
.content-editor { min-width: 0; }
.content-heading-actions { display: flex; flex: 1 1 620px; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 4px; }
.content-order-section { padding: 24px 0; border-bottom: 1px solid #e4e5de; }
.content-section-order { display: grid; max-width: 660px; gap: 5px; }
.content-order-row { display: grid; min-height: 48px; grid-template-columns: 24px 28px minmax(0, 1fr) 76px 36px 36px; align-items: center; gap: 6px; padding: 4px 8px; border: 1px solid #e4e5de; border-radius: 6px; background: #fff; }
.content-order-row[draggable="true"] { cursor: grab; }
.content-order-row:active { cursor: grabbing; }
.drag-handle { color: #8b9891; }
.order-number { color: #8b9891; font-size: 12px; }
.order-label { min-width: 0; color: #28352f; font-size: 13px; font-weight: 600; }
.content-panels { margin-top: 20px; border: 1px solid #e4e5de; border-radius: 6px; background: #fff; }
.content-panels :deep(.v-expansion-panel) { border-bottom: 1px solid #e8e9e3; }
.content-panels :deep(.v-expansion-panel-title) { min-height: 56px; font-weight: 700; }
.skill-group-panels { margin: 0 0 12px; border: 1px solid #e8e9e3; border-radius: 4px; }
.skill-group-panels :deep(.v-expansion-panel-title) { min-height: 48px; font-size: 13px; }
.skill-group-panels :deep(.v-expansion-panel-text__wrapper) { padding: 16px 16px 8px; }
.skill-group-expand-icon { margin-left: 4px; transition: transform 180ms ease; }
.skill-group-panels :deep(.v-expansion-panel-title--active .skill-group-expand-icon) { transform: rotate(180deg); }
.skill-visibility { margin-top: -10px; }
.panel-count { margin-left: 8px; color: #78847e; font-size: 12px; font-weight: 500; }
.editor-entry { padding: 17px 0 12px; border-bottom: 1px solid #e8e9e3; }
.editor-entry:first-child { padding-top: 0; }
.entry-heading { display: flex; min-height: 36px; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.entry-heading h3, .entry-heading h4 { margin: 0; color: #28352f; font: 700 14px/1.4 'Manrope', sans-serif; }
.entry-heading h4 { font-size: 13px; }
.entry-actions { display: flex; align-items: center; justify-content: flex-end; flex: 0 0 auto; }
.content-field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 14px; }
.project-description-field { grid-column: 1 / -1; }
.project-visibility-toggle { grid-column: 1 / -1; }
.language-entry { padding-bottom: 12px; }
.language-detail-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0 14px; padding-left: 12px; }
.nested-editor { margin: 14px 0 4px; padding: 14px; border-left: 3px solid #d9e6dc; background: #f8faf8; }
.nested-entry { padding: 12px 0; border-top: 1px solid #e4e5de; }
.nested-entry > .entry-heading { color: #52635a; font-size: 12px; }
.skill-entry, .list-entry { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto; align-items: start; }
.content-save-actions { padding: 24px 0; }
@media (max-width: 680px) {
  .cv-content-view .page-heading { flex-wrap: wrap; }
  .cv-content-view .content-heading-actions { flex-basis: 100%; align-items: center; justify-content: flex-start; flex-direction: row; }
  .content-order-row { grid-template-columns: 20px 22px minmax(0, 1fr) 66px 32px 32px; gap: 2px; padding: 4px; }
  .content-field-grid, .skill-entry, .list-entry { grid-template-columns: minmax(0, 1fr); gap: 0; }
  .language-detail-grid { grid-template-columns: minmax(0, 1fr); gap: 0; padding-left: 0; }
  .skill-entry .entry-actions, .list-entry .entry-actions { justify-content: flex-end; margin: -10px 0 8px; }
  .nested-editor { padding: 10px; }
}
@media (max-width: 980px) {
  .content-workspace { grid-template-columns: minmax(0, 1fr); gap: 26px; }
  .content-workspace > .preview-panel { grid-row: 1; }
}
</style>
