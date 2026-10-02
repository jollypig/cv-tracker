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
@Table(name = "cv_language")
public class CvLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_id", nullable = false)
    private Cv cv;

    @Column(nullable = false, length = 100)
    private String language;

    @Column(length = 50)
    private String level;

    @Column(length = 50)
    private String reading;

    @Column(length = 50)
    private String writing;

    @Column(length = 50)
    private String speaking;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CvLanguage() {
    }

    public CvLanguage(Cv cv, String language) {
        this.cv = cv;
        this.language = language;
    }

    public UUID getId() { return id; }
    public Cv getCv() { return cv; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getReading() { return reading; }
    public void setReading(String reading) { this.reading = reading; }
    public String getWriting() { return writing; }
    public void setWriting(String writing) { this.writing = writing; }
    public String getSpeaking() { return speaking; }
    public void setSpeaking(String speaking) { this.speaking = speaking; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}