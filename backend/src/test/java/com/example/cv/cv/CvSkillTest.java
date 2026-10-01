package com.example.cv.cv;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CvSkillTest {

    @Test
    void mapsSkillToGroupAndSupportsEditing() throws NoSuchFieldException {
        CvSkillGroup group = new CvSkillGroup(null, "Backend");
        CvSkill skill = new CvSkill(group, "Java");

        skill.setName("Spring Boot");
        skill.setLevel("Advanced");
        skill.setSortOrder(2);

        assertThat(skill.getSkillGroup()).isSameAs(group);
        assertThat(skill.getName()).isEqualTo("Spring Boot");
        assertThat(skill.getLevel()).isEqualTo("Advanced");
        assertThat(skill.getSortOrder()).isEqualTo(2);
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
    }
}