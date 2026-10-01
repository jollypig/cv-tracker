package com.example.cv.cv;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CvExperienceTest {

    @Test
    void mapsExperienceToCvAndSupportsEditing() throws NoSuchFieldException {
        Cv cv = new Cv(null, "Resume", "en", CvStatus.DRAFT);
        CvExperience experience = new CvExperience(cv, "Example", "Engineer");

        experience.setLocation("Remote");
        experience.setStartDate(LocalDate.of(2022, 1, 1));
        experience.setEndDate(LocalDate.of(2024, 1, 1));
        experience.setCurrent(true);
        experience.setDescription("Built a product");
        experience.setSortOrder(2);
        experience.setCompany("New company");
        experience.setPosition("Lead Engineer");

        assertThat(experience.getCv()).isSameAs(cv);
        assertThat(experience.getCompany()).isEqualTo("New company");
        assertThat(experience.getPosition()).isEqualTo("Lead Engineer");
        assertThat(experience.getLocation()).isEqualTo("Remote");
        assertThat(experience.getStartDate()).isEqualTo(LocalDate.of(2022, 1, 1));
        assertThat(experience.getEndDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(experience.isCurrent()).isTrue();
        assertThat(experience.getDescription()).isEqualTo("Built a product");
        assertThat(experience.getSortOrder()).isEqualTo(2);
        assertThat(CvExperience.class.getAnnotation(Table.class).name()).isEqualTo("cv_experience");

        Field cvField = CvExperience.class.getDeclaredField("cv");
        assertThat(cvField.getAnnotation(JoinColumn.class).name()).isEqualTo("cv_id");
        assertThat(cvField.getAnnotation(JoinColumn.class).nullable()).isFalse();
        assertThat(CvExperience.class.getDeclaredField("sortOrder").getAnnotation(Column.class).name())
                .isEqualTo("sort_order");
    }

    @Test
    void defaultsOptionalFieldsAndOrder() {
        CvExperience experience = new CvExperience(null, "Example", "Engineer");

        assertThat(experience.getLocation()).isNull();
        assertThat(experience.getStartDate()).isNull();
        assertThat(experience.getEndDate()).isNull();
        assertThat(experience.isCurrent()).isFalse();
        assertThat(experience.getDescription()).isNull();
        assertThat(experience.getSortOrder()).isZero();
    }
}