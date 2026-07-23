package com.impactbudget.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/** Upload a bank CSV export (Capital One format) to import transactions for the current user. */
@RestController
@RequestMapping("/api/v1/import")
class ImportController {

    private final CapitalOneImportService importService;

    ImportController(CapitalOneImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/capital-one")
    ImportResult importCapitalOne(@AuthenticationPrincipal String userId,
                                  @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is empty");
        }
        try {
            return new ImportResult(importService.importCsv(userId, file.getInputStream()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the CSV: " + e.getMessage());
        }
    }

    record ImportResult(int imported) {
    }
}
