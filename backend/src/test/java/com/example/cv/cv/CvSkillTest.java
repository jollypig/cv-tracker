package com.example.cv.cv;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CvSkillTest {

    @Test
    void roundsCalculatedExperienceUpWithoutChangingEnteredValues() {
        var details = new CvSkillDetails(new java.math.BigDecimal("14.76"), null, null, null,
                null, null, null, true);
        var today = java.time.LocalDate.of(2026, 10, 4);
        var result = details.calculate(java.util.List.of(), today);
        assertThat(result.yearsOfExperience()).isEqualByComparingTo("15");
        assertThat(result.totalExperience()).isEqualByComparingTo("15");
        assertThat(details.yearsOfExperience()).isEqualByComparingTo("14.76");
        var inferred = new CvSkillDetails(null, null, "2024-10-04", "2010-01-01",
                null, null, null, true).calculate(java.util.List.of(), today);
        assertThat(inferred.yearsOfExperience()).isEqualByComparingTo("15");
        assertThat(inferred.totalExperience()).isEqualByComparingTo("15");
    }

    @Test
    void calculatesExperienceAndRecencyFromLinkedPeriods() {
    var details = new CvSkillDetails(new java.math.BigDecimal("2"), null, "2016", "2010",
        "daily", "active", java.util.List.of(new CvSkillDetails.ProjectLink("project", "Shipped")), true);
    var result = details.calculate(java.util.List.of(
        new CvSkillDetails.ProjectPeriod("project", java.time.LocalDate.of(2020, 1, 1),
            java.time.LocalDate.of(2024, 1, 1), false),
        new CvSkillDetails.ProjectPeriod("other", java.time.LocalDate.of(2000, 1, 1), null, true)),
        java.time.LocalDate.of(2026, 10, 4));
    assertThat(result.yearsOfExperience()).isEqualByComparingTo("2");
    assertThat(result.totalExperience()).isEqualByComparingTo("4");
    assertThat(result.lastUsed()).isEqualTo(java.time.LocalDate.of(2024, 1, 1));
    assertThat(result.stale()).isFalse();
    }

    @Test
    void infersYearsAndFlagsStaleSkillsWithoutInventingMissingExperience() {
    var details = new CvSkillDetails(null, null, "2018", "2010", null, null, null, false);
    var result = details.calculate(java.util.List.of(), java.time.LocalDate.of(2026, 10, 4));
    assertThat(result.yearsOfExperience()).isEqualByComparingTo("8");
    assertThat(result.totalExperience()).isEqualByComparingTo("8");
    assertThat(result.stale()).isTrue();
    assertThat(new CvSkillDetails(null, null, null, null, null, null, null, false)
        .calculate(java.util.List.of(), java.time.LocalDate.of(2026, 10, 4)).totalExperience()).isNull();
    assertThat(new CvSkillDetails(null, null, "2024-02-30", "2010", null, null, null, false)
        .isDatesValid()).isFalse();
    }

    @Test
    void sumsOverlapsOncePerLinkedProjectAndUsesTodayForCurrentProjects() {
    var details = new CvSkillDetails(java.math.BigDecimal.ZERO, null, null, null, null, null,
        java.util.List.of(new CvSkillDetails.ProjectLink("first", null),
            new CvSkillDetails.ProjectLink("second", null), new CvSkillDetails.ProjectLink("first", null)), false);
    var today = java.time.LocalDate.of(2024, 1, 1);
    var result = details.calculate(java.util.List.of(
        new CvSkillDetails.ProjectPeriod("first", java.time.LocalDate.of(2020, 1, 1), java.time.LocalDate.of(2022, 1, 1), false),
        new CvSkillDetails.ProjectPeriod("second", java.time.LocalDate.of(2021, 1, 1), null, true)), today);
    assertThat(result.yearsOfExperience()).isEqualByComparingTo("0");
    assertThat(result.totalExperience()).isEqualByComparingTo("5");
    assertThat(result.lastUsed()).isEqualTo(today);
    }

    @Test
    void mapsSkillToGroupAndSupportsEditing() throws NoSuchFieldException {
        CvSkillGroup group = new CvSkillGroup(null, "Backend");
        CvSkill skill = new CvSkill(group, "Java");

        skill.setName("Spring Boot");
        skill.setLevel("Advanced");
        skill.setSortOrder(2);
        skill.setVisible(false);

        assertThat(skill.getSkillGroup()).isSameAs(group);
        assertThat(skill.getName()).isEqualTo("Spring Boot");
        assertThat(skill.getLevel()).isEqualTo("Advanced");
        assertThat(skill.getSortOrder()).isEqualTo(2);
        assertThat(skill.isVisible()).isFalse();
        assertThat(CvSkill.class.getAnnotation(Table.class).name()).isEqualTo("cv_skill");
        assertThat(CvSkill.class.getDeclaredField("skillGroup").getAnnotation(JoinColumn.class).name())
                .isEqualTo("skill_group_id");
        assertThat(CvSkill.class.getDeclaredField("skillGroup").getAnnotation(JoinColumn.class).nullable())
                .isFalse();
        assertThat(CvSkill.class.getDeclaredField("sortOrder").getAnnotation(Column.class).name())
                .isEqualTo("sort_order");
    }

    @Test
    void defaultsOptionalFieldsAndOrder() {
        CvSkill skill = new CvSkill(null, "Java");

        assertThat(skill.getLevel()).isNull();
        assertThat(skill.getSortOrder()).isZero();
        assertThat(skill.isVisible()).isTrue();
    }
}