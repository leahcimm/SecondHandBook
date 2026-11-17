package ntou.cs.secondhandbook.service;

import ntou.cs.secondhandbook.exception.InvalidBookException;
import ntou.cs.secondhandbook.exception.ResourceNotFoundException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.BookForm;
import ntou.cs.secondhandbook.model.LoginUser;
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
                .filter(b -> isBlank(conditionGrade) || conditionGrade.equals(b.getConditionGrade()))
                .sorted(comparatorFor(sort))
                .toList();
    }

    public Book getBookById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + id));
    }

    // US02：刊登書籍。先檢查資料，全部通過才存照片與資料。賣家就是登入者（T05）
    public Book addBook(BookForm form, LoginUser seller) throws IOException {
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
        book.setSellerId(seller.id());
        book.setSellerName(seller.name());
        book.setSellerContact(seller.contact());
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
