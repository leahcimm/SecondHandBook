package ntou.cs.secondhandbook.controller;

import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.BookForm;
import ntou.cs.secondhandbook.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private CurrentUser currentUser;

    // US03：GET /api/books?keyword=資料&subject=資料結構&conditionGrade=良好&sort=price（參數都可省略）
    // sort：price 價格低到高（預設）、newest 最新刊登、condition 書況由新到舊
    @GetMapping
    public List<Book> getBooks(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "subject", required = false) String subject,
                               @RequestParam(value = "conditionGrade", required = false) String conditionGrade,
                               @RequestParam(value = "sort", required = false) String sort) {
        return bookService.findBooks(keyword, subject, conditionGrade, sort);
    }

    // 我的刊登（T06）：GET /api/books/mine，要登入（SecurityConfig），回自己所有狀態的書和退回原因
    @GetMapping("/mine")
    public List<Book> getMyBooks() {
        return bookService.findMyBooks(currentUser.get());
    }

    // 待審核、已退回的書只有賣家本人和管理員看得到，其他人回 404（T06）
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        return ResponseEntity.ok(bookService.getBookById(id, currentUser.getOrNull()));
    }

    // US02：POST /api/books，用 multipart/form-data 送出表單與照片，欄位對應 BookForm。要登入，賣家就是登入者
    @PostMapping
    public ResponseEntity<Book> addBook(@ModelAttribute BookForm form) throws IOException {
        Book newBook = bookService.addBook(form, currentUser.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(newBook);
    }
}
