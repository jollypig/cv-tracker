package com.example.cv.cv;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CvEducationTest {

    @Test
    void mapsEducationToCvAndSupportsEditing() throws NoSuchFieldException {
        Cv cv = new Cv(null, "Resume", "en", CvStatus.DRAFT);
        CvEducation education = new CvEducation(cv, "University");

        education.setInstitution("New university");
        education.setDegree("MSc");
        education.setFieldOfStudy("Computer Science");
        education.setStartDate(LocalDate.of(2018, 9, 1));
        education.setEndDate(LocalDate.of(2020, 6, 1));
        education.setDescription("Graduated with honors");
        education.setSortOrder(3);

        assertThat(education.getCv()).isSameAs(cv);
        assertThat(education.getInstitution()).isEqualTo("New university");
        assertThat(education.getDegree()).isEqualTo("MSc");
        assertThat(education.getFieldOfStudy()).isEqualTo("Computer Science");
        assertThat(education.getStartDate()).isEqualTo(LocalDate.of(2018, 9, 1));
        assertThat(education.getEndDate()).isEqualTo(LocalDate.of(2020, 6, 1));
        assertThat(education.getDescription()).isEqualTo("Graduated with honors");
        assertThat(education.getSortOrder()).isEqualTo(3);
        assertThat(CvEducation.class.getAnnotation(Table.class).name()).isEqualTo("cv_education");
        assertThat(CvEducation.class.getDeclaredField("cv").getAnnotation(JoinColumn.class).nullable())
                .isFalse();
        assertThat(CvEducation.class.getDeclaredField("fieldOfStudy").getAnnotation(Column.class).name())
                .isEqualTo("field_of_study");
    }

    @Test
    void defaultsOptionalFieldsAndOrder() {
        CvEducation education = new CvEducation(null, "University");

        assertThat(education.getDegree()).isNull();
        assertThat(education.getFieldOfStudy()).isNull();
        assertThat(education.getStartDate()).isNull();
        assertThat(education.getEndDate()).isNull();
        assertThat(education.getDescription()).isNull();
        assertThat(education.getSortOrder()).isZero();
    }
}