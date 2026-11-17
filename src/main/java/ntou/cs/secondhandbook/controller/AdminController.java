package ntou.cs.secondhandbook.controller;

import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.RejectForm;
import ntou.cs.secondhandbook.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// US07 審核書籍（FR-AD-01）。只有管理員能用：SecurityConfig 把 /api/admin/** 限定給 ROLE_ADMIN，
// 沒登入回 401、不是管理員回 403。
//   GET  /api/admin/books                待審核的書，先刊登的在前
//   POST /api/admin/books/{id}/approve   核准 → 上架中
//   POST /api/admin/books/{id}/reject    JSON {"reason":"..."}，退回 → 已退回；原因留白回 400、狀態不變
// 只有「待審核」的書能核准或退回，其他狀態回 400。
@RestController
@RequestMapping("/api/admin/books")
public class AdminController {

    @Autowired
    private BookService bookService;

    @GetMapping
    public List<Book> getPendingBooks() {
        return bookService.findPendingBooks();
    }

    @PostMapping("/{id}/approve")
    public Book approve(@PathVariable String id) {
        return bookService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public Book reject(@PathVariable String id, @RequestBody(required = false) RejectForm form) {
        return bookService.reject(id, form == null ? null : form.reason());
    }
}
