package com.fleta.closet.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ExceptionTestController {

    @GetMapping("/test/app-exception")
    void throwAppException() {
        throw AppException.clothingNotFound();
    }

    @PostMapping("/test/validation")
    void throwValidationException(@Valid @RequestBody TestRequest request) {
    }

    @GetMapping("/test/general-exception")
    void throwGeneralException() {
        throw new RuntimeException("unexpected");
    }

    record TestRequest(@NotBlank @Email String email) {
    }
}
