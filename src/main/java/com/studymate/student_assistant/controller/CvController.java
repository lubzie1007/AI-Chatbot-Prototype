package com.studymate.student_assistant.controller;

import com.studymate.student_assistant.model.CvDraft;
import com.studymate.student_assistant.service.CvPdfService;
import com.studymate.student_assistant.service.CvService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cv")
public class CvController {

    private final CvService cvService;
    private final CvPdfService pdfService;

    public CvController(CvService cvService, CvPdfService pdfService) {
        this.cvService = cvService;
        this.pdfService = pdfService;
    }

    /** Kick off the CV builder for a conversation. */
    @PostMapping("/{conversationId}/start")
    public Map<String, Object> start(@PathVariable String conversationId) {
        CvDraft draft = cvService.startOrGet(conversationId);
        String question = cvService.questionFor(draft.getCurrentSlot());
        return Map.of(
            "reply", question,
            "cvMode", true,
            "slot", draft.getCurrentSlot()
        );
    }

    /** Store the user's answer for the current slot, ask the next question. */
    @PostMapping("/{conversationId}/answer")
    public Map<String, Object> answer(@PathVariable String conversationId,
                                      @RequestBody Map<String, String> body) {
        String answer = body.getOrDefault("answer", "").trim();

        Optional<CvDraft> opt = cvService.get(conversationId);
        if (opt.isEmpty()) {
            return Map.of("reply", "CV session not started. Type \"build my CV\" first.");
        }
        CvDraft draft = opt.get();
        cvService.recordAnswer(draft, answer);

        if (draft.isComplete()) {
            return Map.of(
                "reply", "🎉 Your CV is ready! Click the button below to download it.",
                "cvMode", true,
                "slot", "done",
                "complete", true,
                "downloadUrl", "/api/cv/" + conversationId + "/download"
            );
        }

        return Map.of(
            "reply", cvService.questionFor(draft.getCurrentSlot()),
            "cvMode", true,
            "slot", draft.getCurrentSlot()
        );
    }

    /** Download the generated CV as PDF. */
    @GetMapping("/{conversationId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String conversationId) throws Exception {
        Optional<CvDraft> opt = cvService.get(conversationId);
        if (opt.isEmpty() || !opt.get().isComplete()) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = pdfService.generate(opt.get());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition
            .attachment()
            .filename("CV_" + opt.get().getFullName().replaceAll("\\s+", "_") + ".pdf")
            .build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
