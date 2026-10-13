package ntou.cs.secondhandbook.service;

import ntou.cs.secondhandbook.exception.InvalidBookException;
import ntou.cs.secondhandbook.exception.ResourceNotFoundException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class BookService {

    // 允許的照片格式：JPG、PNG、WEBP
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    // 最新刊登的排最前面
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    @Autowired
    private BookRepository repository;

    @Autowired
    private FileStorageService fileStorageService;

    // US03：瀏覽書籍列表，可用書名關鍵字與科目篩選（兩個都可以不填）
    public List<Book> findBooks(String keyword, String subject) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasSubject = subject != null && !subject.isBlank();

        if (hasKeyword && hasSubject) {
            return repository.findByTitleContainingIgnoreCaseAndSubject(keyword.trim(), subject, NEWEST_FIRST);
        }
        if (hasKeyword) {
            return repository.findByTitleContainingIgnoreCase(keyword.trim(), NEWEST_FIRST);
        }
        if (hasSubject) {
            return repository.findBySubject(subject, NEWEST_FIRST);
        }
        return repository.findAll(NEWEST_FIRST);
    }

    public Book getBookById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + id));
    }

    // US02：刊登書籍。先檢查資料，全部通過才存照片與資料
    public Book addBook(String title, String author, String subject, String condition, String price,
                        String sellerName, String sellerContact, MultipartFile image) throws IOException {
        if (isBlank(title)) {
            throw new InvalidBookException("請填寫書名");
        }
        if (isBlank(subject)) {
            throw new InvalidBookException("請選擇科目");
        }
        int parsedPrice = parsePrice(price);
        if (image == null || image.isEmpty()) {
            throw new InvalidBookException("請上傳書籍照片");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
            throw new InvalidBookException("照片格式只接受 JPG、PNG、WEBP");
        }

        String fileName = fileStorageService.storeFile(image);

        Book book = new Book();
        book.setTitle(title.trim());
        book.setAuthor(trimToNull(author));
        book.setSubject(subject.trim());
        book.setCondition(trimToNull(condition));
        book.setPrice(parsedPrice);
        book.setImageUrl("/uploads/" + fileName);
        book.setSellerName(trimToNull(sellerName));
        book.setSellerContact(trimToNull(sellerContact));
        book.setStatus(Book.STATUS_PENDING);
        book.setCreatedAt(LocalDateTime.now());

        return repository.save(book);
    }

    // 售價必須是正整數（例如 250），-50、0、abc、12.5 都不行
    private int parsePrice(String price) {
        if (isBlank(price)) {
            throw new InvalidBookException("請填寫售價");
        }
        try {
            int value = Integer.parseInt(price.trim());
            if (value <= 0) {
                throw new InvalidBookException("售價必須為正整數");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidBookException("售價必須為正整數");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
