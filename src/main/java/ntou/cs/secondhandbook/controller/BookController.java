package ntou.cs.secondhandbook.controller;

import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    // US03：GET /api/books?keyword=資料&subject=資料結構（兩個參數都可省略）
    @GetMapping
    public List<Book> getBooks(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "subject", required = false) String subject) {
        return bookService.findBooks(keyword, subject);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    // US02：POST /api/books，用 multipart/form-data 送出表單與照片
    @PostMapping
    public ResponseEntity<Book> addBook(@RequestParam(value = "title", required = false) String title,
                                        @RequestParam(value = "author", required = false) String author,
                                        @RequestParam(value = "subject", required = false) String subject,
                                        @RequestParam(value = "condition", required = false) String condition,
                                        @RequestParam(value = "price", required = false) String price,
                                        @RequestParam(value = "sellerName", required = false) String sellerName,
                                        @RequestParam(value = "sellerContact", required = false) String sellerContact,
                                        @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        Book newBook = bookService.addBook(title, author, subject, condition, price, sellerName, sellerContact, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(newBook);
    }
}
