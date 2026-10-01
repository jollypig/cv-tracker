package com.example.cv.cv;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "cv_skill")
public class CvSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_group_id", nullable = false)
    private CvSkillGroup skillGroup;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 50)
    private String level;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CvSkill() {
    }

    public CvSkill(CvSkillGroup skillGroup, String name) {
        this.skillGroup = skillGroup;
        this.name = name;
    }

    public UUID getId() { return id; }
    public CvSkillGroup getSkillGroup() { return skillGroup; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}