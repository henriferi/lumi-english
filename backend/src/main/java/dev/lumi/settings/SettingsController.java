package dev.lumi.settings;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
class SettingsController {

    private final SettingsService service;

    SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    SettingsResponse get() {
        return service.get();
    }

    @PutMapping
    SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        return service.update(request);
    }
}
