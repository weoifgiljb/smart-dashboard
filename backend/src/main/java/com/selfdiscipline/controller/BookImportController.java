package com.selfdiscipline.controller;

import com.selfdiscipline.dto.BookImportRequest;
import com.selfdiscipline.dto.MessageResponse;
import com.selfdiscipline.service.BookImportService;
import com.selfdiscipline.util.RemoteUrlGuard;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books/import")
public class BookImportController {

    private final BookImportService bookImportService;

    public BookImportController(BookImportService bookImportService) {
        this.bookImportService = bookImportService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> importBooks(@Valid @RequestBody BookImportRequest request) {
        RemoteUrlGuard.assertSafeHttpUrl(request.getCsvUrl());
        int limit = request.getLimit() != null ? request.getLimit() : 100;
        bookImportService.importBooksFromUrl(request.getCsvUrl(), limit);
        return ResponseEntity.ok(new MessageResponse("已开始后台导入，稍后刷新书架"));
    }

    @PostMapping("/sample")
    public ResponseEntity<MessageResponse> importSampleShelf() {
        int count = bookImportService.importSampleShelf();
        if (count == 0) {
            return ResponseEntity.ok(new MessageResponse("示例书架已经导入过了"));
        }
        return ResponseEntity.ok(new MessageResponse("已放入 " + count + " 本示例书"));
    }
}
