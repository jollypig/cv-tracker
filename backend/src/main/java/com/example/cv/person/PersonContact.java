package com.example.cv.person;

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
@Table(name = "person_contact")
public class PersonContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 500)
    private String value;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "show_contact", nullable = false)
    private boolean showContact = true;

    protected PersonContact() {
    }

    public PersonContact(String type, String value, boolean primary, int sortOrder) {
        this(type, value, primary, sortOrder, true);
    }

    public PersonContact(String type, String value, boolean primary, int sortOrder, boolean showContact) {
        this.type = type;
        this.value = value;
        this.primary = primary;
        this.sortOrder = sortOrder;
        this.showContact = showContact;
    }

    public UUID getId() { return id; }
    public Person getPerson() { return person; }
    void setPerson(Person person) { this.person = person; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isShowContact() { return showContact; }
    public void setShowContact(boolean showContact) { this.showContact = showContact; }
}