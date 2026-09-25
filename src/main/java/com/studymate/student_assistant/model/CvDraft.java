package com.studymate.student_assistant.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cv_draft")
public class CvDraft {

    @Id
    @Column(length = 36)
    private String conversationId;

    private String fullName;
    private String contact;
    private String school;
    private String subjects;
    private String experience;
    private String skills;
    private String activities;

    /** Which field we're currently waiting for. */
    @Column(length = 30)
    private String currentSlot;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public CvDraft() {}

    public CvDraft(String conversationId) {
        this.conversationId = conversationId;
        this.currentSlot = "fullName";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isComplete() {
        return fullName != null && contact != null && school != null
            && subjects != null && experience != null && skills != null
            && activities != null;
    }

    // ---- getters / setters ----
    public String getConversationId() { return conversationId; }
    public void setConversationId(String c) { this.conversationId = c; }

    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }

    public String getContact() { return contact; }
    public void setContact(String v) { this.contact = v; }

    public String getSchool() { return school; }
    public void setSchool(String v) { this.school = v; }

    public String getSubjects() { return subjects; }
    public void setSubjects(String v) { this.subjects = v; }

    public String getExperience() { return experience; }
    public void setExperience(String v) { this.experience = v; }

    public String getSkills() { return skills; }
    public void setSkills(String v) { this.skills = v; }

    public String getActivities() { return activities; }
    public void setActivities(String v) { this.activities = v; }

    public String getCurrentSlot() { return currentSlot; }
    public void setCurrentSlot(String s) { this.currentSlot = s; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
