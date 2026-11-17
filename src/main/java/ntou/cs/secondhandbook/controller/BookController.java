package ntou.cs.secondhandbook.controller;

import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.BookForm;
import ntou.cs.secondhandbook.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @Value("${app.admin.key:dev-admin-12345}")
    private String adminKey;

    // US03：GET /api/books?keyword=資料&subject=資料結構&conditionGrade=良好&sort=price（參數都可省略）
    // sort：price 價格低到高（預設）、newest 最新刊登、condition 書況由新到舊
    @GetMapping
    public List<Book> getBooks(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "subject", required = false) String subject,
                               @RequestParam(value = "conditionGrade", required = false) String conditionGrade,
                               @RequestParam(value = "sort", required = false) String sort) {
        return bookService.findBooks(keyword, subject, conditionGrade, sort);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    // US02：POST /api/books，用 multipart/form-data 送出表單與照片，欄位對應 BookForm
    @PostMapping
    public ResponseEntity<Book> addBook(@ModelAttribute BookForm form) throws IOException {
        Book newBook = bookService.addBook(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(newBook);
    }
    
    // US09：取得其他賣家的同名書籍
    // GET /api/books/{id}/similar
    @GetMapping("/{id}/similar")
    public List<Book> getSimilarBooks(@PathVariable String id) {
        return bookService.findSimilarBooks(id);
    }

    // US10：取得指定賣家的所有刊登
    // GET /api/books/seller/{sellerName}
    @GetMapping("/seller/{sellerName}")
    public List<Book> getMyBooks(@PathVariable String sellerName) {
        return bookService.findMyBooks(sellerName);
    }

    // US10：下架指定書籍
    // PATCH /api/books/{id}/delist?sellerName=王小明
    @PatchMapping("/{id}/delist")
    public ResponseEntity<Book> delistBook(@PathVariable String id, @RequestParam String sellerName) {
        Book updatedBook = bookService.delistBook(id, sellerName);
        return ResponseEntity.ok(updatedBook);
    }
    // ==================== US07 審核書籍 ====================

    // 查詢待審核書籍
    @GetMapping("/admin/pending")
    public List<Book> getPendingBooks(@RequestHeader(value = "X-Admin-Key", required = false) String providedKey) {
        checkAdmin(providedKey);
        return bookService.findPendingBooks();
    }

    // 核准書籍
    @PatchMapping("/{id}/approve")
    public Book approveBook(@PathVariable String id, @RequestHeader(value = "X-Admin-Key", required = false) String providedKey) {
        checkAdmin(providedKey);
        return bookService.approveBook(id);
    }

    // 退回書籍
    @PatchMapping("/{id}/return")
    public Book returnBook(@PathVariable String id, @RequestBody Map<String, String> request, @RequestHeader(value = "X-Admin-Key", required = false) String providedKey) {
        checkAdmin(providedKey);
        return bookService.returnBook(id, request.get("reason"));
    }
    
    // 管理員驗證(暫時)
    private void checkAdmin(String providedKey) {
        if (providedKey == null || !MessageDigest.isEqual(adminKey.getBytes(StandardCharsets.UTF_8), providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理員可以執行此操作");
        }
    }

}
