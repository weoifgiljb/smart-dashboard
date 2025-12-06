package com.selfdiscipline.controller;

import com.selfdiscipline.dto.BookImportRequest;
import com.selfdiscipline.service.BookImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books/import")
public class BookImportController {

    @Autowired
    private BookImportService bookImportService;

    @PostMapping
    public ResponseEntity<String> importBooks(@RequestBody BookImportRequest request) {
        String url = request.getCsvUrl();
        if (url == null || url.isEmpty()) {
            // Default to a known raw URL if user doesn't provide one (example placeholder)
            // For now, require URL
            return ResponseEntity.badRequest().body("CSV URL is required");
        }
        
        int limit = request.getLimit() != null ? request.getLimit() : 100; // Default limit 100 for safety
        
        bookImportService.importBooksFromUrl(url, limit);
        return ResponseEntity.ok("Import started in background...");
    }
}




