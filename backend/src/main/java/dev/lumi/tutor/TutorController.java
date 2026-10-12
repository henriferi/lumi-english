package dev.lumi.tutor;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
class TutorController {

    private final TutorService service;

    TutorController(TutorService service) {
        this.service = service;
    }

    @PostMapping
    ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return service.chat(request);
    }
}
