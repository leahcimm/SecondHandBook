package ntou.cs.secondhandbook.service;

import ntou.cs.secondhandbook.exception.InvalidBookException;
import ntou.cs.secondhandbook.exception.ResourceNotFoundException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.BookForm;
import ntou.cs.secondhandbook.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class BookService {

    // 允許的照片格式：JPG、PNG、WEBP
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    // 列表排序方式（GET /api/books?sort=...），沒給或給錯就用 price
    public static final String SORT_PRICE = "price";         // 價格由低到高（預設，方便比較同一本書）
    public static final String SORT_NEWEST = "newest";       // 最新刊登的在前
    public static final String SORT_CONDITION = "condition"; // 書況由新到舊，同等級再比價格

    private static final Comparator<Book> NEWEST_FIRST =
            Comparator.comparing(Book::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private static final Comparator<Book> CHEAPEST_FIRST =
            Comparator.comparingInt(Book::getPrice).thenComparing(NEWEST_FIRST);

    // 沒有書況等級的舊資料排最後
    private static final Comparator<Book> BEST_CONDITION_FIRST =
            Comparator.comparingInt((Book b) -> gradeRank(b.getConditionGrade())).thenComparing(CHEAPEST_FIRST);

    @Autowired
    private BookRepository repository;

    @Autowired
    private FileStorageService fileStorageService;

    // US03：瀏覽書籍列表，可用書名關鍵字、科目、書況等級篩選（都可以不填），再依 sort 排序
    public List<Book> findBooks(String keyword, String subject, String conditionGrade, String sort) {
        boolean hasKeyword = !isBlank(keyword);
        boolean hasSubject = !isBlank(subject);

        List<Book> books;
        if (hasKeyword && hasSubject) {
            books = repository.findByTitleContainingIgnoreCaseAndSubject(keyword.trim(), subject);
        } else if (hasKeyword) {
            books = repository.findByTitleContainingIgnoreCase(keyword.trim());
        } else if (hasSubject) {
            books = repository.findBySubject(subject);
        } else {
            books = repository.findAll();
        }

        return books.stream()
                .filter(b -> Book.STATUS_ACTIVE.equals(b.getStatus()))
                .filter(b -> isBlank(conditionGrade) || conditionGrade.equals(b.getConditionGrade()))
                .sorted(comparatorFor(sort))
                .toList();
    }

    public Book getBookById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + id));
    }
    
    // US09: 了解書籍資訊，用於同名書籍查詢
    public List<Book> findSimilarBooks(String id) {
        Book currentBook = getBookById(id);

        return repository.findByTitleAndAuthorAndStatus(currentBook.getTitle(), currentBook.getAuthor(), Book.STATUS_ACTIVE)
                        .stream()
                        .filter(book -> !book.getId().equals(currentBook.getId()))
                        .sorted(Comparator.comparingInt(Book::getPrice))
                        .toList();       
    }
    
    // US10: 下架書籍: 賣家可手動下架「上架中」的書籍
    // 1.取得指定賣家的所有刊登紀錄
    public List<Book> findMyBooks(String sellerName) {
        if (isBlank(sellerName)) {
            throw new InvalidBookException("請提供賣家名稱");
        }

        // 查詢該賣家的所有書籍，包含已下架及待審核
        return repository.findBySellerName(sellerName.trim())
                .stream()
                .sorted(NEWEST_FIRST)
                .toList();
    }
    
    // 2. 下架指定書籍(目前先指定賣家)
    public Book delistBook(String id, String sellerName) {
        Book book = getBookById(id);

        if (isBlank(sellerName)) {
            throw new InvalidBookException("請提供賣家名稱");
        }
        if (!sellerName.trim().equals(book.getSellerName())) {
            throw new ResourceNotFoundException("找不到此賣家的書籍");
        }
        if (!Book.STATUS_ACTIVE.equals(book.getStatus())) {
            throw new InvalidBookException("只有上架中的書籍可以下架");
        }

        book.setStatus(Book.STATUS_DELISTED);
        return repository.save(book);
    }

    // US07 審核書籍
    // 1. 查詢所有待審核的書籍
    public List<Book> findPendingBooks() {
        return repository.findByStatus(Book.STATUS_PENDING)
                        .stream()
                        .sorted(NEWEST_FIRST)
                        .toList();
    }

    // 2. 管理員核准書籍
    public Book approveBook(String id) {
        Book book = getBookById(id);

        if (!Book.STATUS_PENDING.equals(book.getStatus())) {
            throw new InvalidBookException("只有待審核的書籍可以核准");
        }

        book.setStatus(Book.STATUS_ACTIVE);
        book.setReviewReason(null);
        return repository.save(book);
    }

    // 3. 管理員退回書籍
    public Book returnBook(String id, String reason) {
        Book book = getBookById(id);

        if (isBlank(reason)) {
            throw new InvalidBookException("退回原因不可為空白");
        }
        if (!Book.STATUS_PENDING.equals(book.getStatus())) {
            throw new InvalidBookException("只有待審核的書籍可以退回");
        }

        book.setStatus(Book.STATUS_RETURNED);
        book.setReviewReason(reason.trim());
        return repository.save(book);
    }

    // US02：刊登書籍。先檢查資料，全部通過才存照片與資料
    public Book addBook(BookForm form) throws IOException {
        if (isBlank(form.getTitle())) {
            throw new InvalidBookException("請填寫書名");
        }
        if (isBlank(form.getSubject())) {
            throw new InvalidBookException("請選擇科目");
        }
        if (isBlank(form.getConditionGrade()) || !Book.CONDITION_GRADES.contains(form.getConditionGrade())) {
            throw new InvalidBookException("請選擇書況");
        }
        int parsedPrice = parsePrice(form.getPrice());
        // 校內面交一律可以；勾選可寄送時才需要運費
        Integer shippingFee = form.isShippable() ? parseShippingFee(form.getShippingFee()) : null;
        MultipartFile image = form.getImage();
        if (image == null || image.isEmpty()) {
            throw new InvalidBookException("請上傳書籍照片");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
            throw new InvalidBookException("照片格式只接受 JPG、PNG、WEBP");
        }

        String fileName = fileStorageService.storeFile(image);

        Book book = new Book();
        book.setTitle(form.getTitle().trim());
        book.setAuthor(trimToNull(form.getAuthor()));
        book.setSubject(form.getSubject().trim());
        book.setConditionGrade(form.getConditionGrade());
        book.setCondition(trimToNull(form.getCondition()));
        book.setPrice(parsedPrice);
        book.setShippable(form.isShippable());
        book.setShippingFee(shippingFee);
        book.setImageUrl("/uploads/" + fileName);
        book.setSellerName(trimToNull(form.getSellerName()));
        book.setSellerContact(trimToNull(form.getSellerContact()));
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

    // 運費必須是 0 以上的整數（0 代表賣家吸收運費），-1、abc、12.5 都不行
    private int parseShippingFee(String shippingFee) {
        if (isBlank(shippingFee)) {
            throw new InvalidBookException("勾選寄送時請填寫運費");
        }
        try {
            int value = Integer.parseInt(shippingFee.trim());
            if (value < 0) {
                throw new InvalidBookException("運費必須為 0 以上的整數");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidBookException("運費必須為 0 以上的整數");
        }
    }

    private static Comparator<Book> comparatorFor(String sort) {
        if (SORT_NEWEST.equals(sort)) {
            return NEWEST_FIRST;
        }
        if (SORT_CONDITION.equals(sort)) {
            return BEST_CONDITION_FIRST;
        }
        return CHEAPEST_FIRST;
    }

    private static int gradeRank(String grade) {
        // List.of 做的清單不能用 null 查詢，要先擋掉
        int index = grade == null ? -1 : Book.CONDITION_GRADES.indexOf(grade);
        return index >= 0 ? index : Book.CONDITION_GRADES.size();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
