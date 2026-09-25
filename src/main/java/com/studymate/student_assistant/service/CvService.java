package com.studymate.student_assistant.service;

import com.studymate.student_assistant.model.CvDraft;
import com.studymate.student_assistant.repository.CvDraftRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class CvService {

    private final CvDraftRepository repo;

    public CvService(CvDraftRepository repo) {
        this.repo = repo;
    }

    public CvDraft startOrGet(String conversationId) {
        Optional<CvDraft> existing = repo.findById(conversationId);
        if (existing.isPresent()) return existing.get();
        CvDraft draft = new CvDraft(conversationId);
        return repo.save(draft);
    }

    public Optional<CvDraft> get(String conversationId) {
        return repo.findById(conversationId);
    }

    /** The question to ask for the current slot. */
    public String questionFor(String slot) {
        return switch (slot) {
            case "fullName"   -> "Great — let's build your CV! 🎓\n\nFirst, what is your **full name**?";
            case "contact"    -> "Nice to meet you! What is your **phone number and email address**? (e.g. 072 123 4567, lubabalo@email.com)";
            case "school"     -> "Which **school did you attend**, and in what year did you finish (or will you finish) matric?";
            case "subjects"   -> "List your **matric subjects and marks**, one per line. (e.g. Mathematics 75, English 68, ...)";
            case "experience" -> "Do you have any **work, volunteer, or tutoring experience**? If none, just say \"none\".";
            case "skills"     -> "What **skills** would you like to highlight? (e.g. teamwork, coding, public speaking, MS Office)";
            case "activities" -> "Finally, any **extracurriculars, sports, clubs, or achievements**? (Type \"none\" if not applicable)";
            default           -> "Let's continue with your CV.";
        };
    }

    /** Store the user's answer in the current slot, then advance. */
    public void recordAnswer(CvDraft draft, String answer) {
        String slot = draft.getCurrentSlot();
        switch (slot) {
            case "fullName"   -> draft.setFullName(answer.trim());
            case "contact"    -> draft.setContact(answer.trim());
            case "school"     -> draft.setSchool(answer.trim());
            case "subjects"   -> draft.setSubjects(answer.trim());
            case "experience" -> draft.setExperience(answer.trim());
            case "skills"     -> draft.setSkills(answer.trim());
            case "activities" -> draft.setActivities(answer.trim());
        }
        draft.setCurrentSlot(nextSlot(slot));
        draft.setUpdatedAt(Instant.now());
        repo.save(draft);
    }

    private String nextSlot(String slot) {
        return switch (slot) {
            case "fullName"   -> "contact";
            case "contact"    -> "school";
            case "school"     -> "subjects";
            case "subjects"   -> "experience";
            case "experience" -> "skills";
            case "skills"     -> "activities";
            case "activities" -> "done";
            default           -> "done";
        };
    }
}

