package com.example.cv.cv;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CvHtmlRendererTest {

    private final CvHtmlRenderer renderer = new CvHtmlRenderer();

    @Test
    void rendersOptInCalculatedMetadataAndEscapesProjectOutcomes() {
        var details = new CvSkillDetails(new java.math.BigDecimal("2"), new java.math.BigDecimal("1"),
                "2016", "2010", "occasionally", "maintaining",
                List.of(new CvSkillDetails.ProjectLink("project", "Shipped <fast> & safely")), true);
        var content = new CvContent(null, List.of(), List.of(), List.of(new CvContent.SkillGroup("Backend", 0,
                List.of(new CvContent.Skill("Java", "Advanced", 0, true, details)))), List.of(),
                List.of(new CvContent.Project("Platform", null, null, null, null, 0, "project",
                        java.time.LocalDate.of(2020, 1, 1), java.time.LocalDate.of(2024, 1, 1), false)),
                List.of(), List.of(), List.of(new CvContent.Section(CvSectionType.SKILLS, true, 0)));
        assertThat(renderer.render(snapshot(content), "modern"))
                .contains("Total experience: 4 years", "Last used: 2024-01-01", "Actively used: 1 years",
                        "occasionally", "maintaining", "Platform: Shipped &lt;fast&gt; &amp; safely")
                .doesNotContain("<fast>");
    }

    @Test
    void escapesUserContentAndHonorsHiddenSections() {
        CvContent content = content("<script>private & unsafe</script>", List.of(
                new CvContent.Section(CvSectionType.SUMMARY, true, 0),
                new CvContent.Section(CvSectionType.EXPERIENCE, false, 1)));

        String html = renderer.render(snapshot(content), "modern");

        assertThat(html).contains("&lt;script&gt;private &amp; unsafe&lt;/script&gt;")
                .doesNotContain("<script>")
                .doesNotContain("Hidden employer");
    }

    @Test
    void doesNotRenderSectionsWhenEveryConfiguredSectionIsHidden() {
        CvContent content = content("Should not appear", List.of(
                new CvContent.Section(CvSectionType.SUMMARY, false, 0)));

        String html = renderer.render(snapshot(content), "minimal");

        assertThat(html).doesNotContain("Should not appear").doesNotContain("<h2>");
    }

    @Test
    void emitsDocumentTextAndOrdersVisibleSections() {
        CvContent content = content("A careful engineer", List.of(
                new CvContent.Section(CvSectionType.EXPERIENCE, true, 0),
                new CvContent.Section(CvSectionType.SUMMARY, true, 1)));
        CvVersionSnapshot withProfile = new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT,
                content, new CvVersionSnapshot.PersonProfile("Jane", "Doe", "Engineer", "Riga", List.of()));

        String html = renderer.render(withProfile, "classic");

        assertThat(html.indexOf("Experience")).isLessThan(html.indexOf("Profile"));
        assertThat(html).contains("Jane Doe", "Engineer", "Riga", "A careful engineer");
    }

    @Test
    void omitsHiddenSkillsAndGroupsWithoutPrintableSkills() {
        CvContent content = new CvContent(null, List.of(), List.of(), List.of(
                new CvContent.SkillGroup("Backend", 0, List.of(
                        new CvContent.Skill("Java", "Advanced", 0, false),
                        new CvContent.Skill("Kotlin", "Intermediate", 1, null))),
                new CvContent.SkillGroup("Hidden group", 1, List.of(
                        new CvContent.Skill("Rust", null, 0, false)))),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new CvContent.Section(CvSectionType.SKILLS, true, 0)));

        String html = renderer.render(snapshot(content), "modern");

        assertThat(html).contains("Skills", "Backend", "Kotlin (Intermediate)")
                .doesNotContain("Java", "Hidden group", "Rust");
    }

    @Test
    void omitsSkillsSectionWhenEverySkillIsHidden() {
        CvContent content = new CvContent(null, List.of(), List.of(), List.of(
                new CvContent.SkillGroup("Backend", 0, List.of(
                        new CvContent.Skill("Java", null, 0, false)))),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new CvContent.Section(CvSectionType.SKILLS, true, 0)));

        String html = renderer.render(snapshot(content), "modern");

        assertThat(html).doesNotContain("Skills", "Backend", "Java");
    }

    private CvVersionSnapshot snapshot(CvContent content) {
        return new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT, content, null);
    }

    private CvContent content(String summary, List<CvContent.Section> sections) {
        CvContent.Experience experience = new CvContent.Experience("Hidden employer", "Role", null, null, null,
                null, null, false, null, 0, List.of());
        return new CvContent(summary, List.of(experience), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), sections);
    }
}