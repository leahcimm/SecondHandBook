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

    // US03：瀏覽書籍列表，可用書名關鍵字、科目、書況等級篩選（都可以不填），再依 sort 排序。
    // T06：只顯示「上架中」的書；待審核、已退回、保留中、已售出都不出現在列表
    public List<Book> findBooks(String keyword, String subject, String conditionGrade, String sort) {
        boolean hasKeyword = !isBlank(keyword);
        boolean hasSubject = !isBlank(subject);
        String status = Book.STATUS_ON_SALE;

        List<Book> books;
        if (hasKeyword && hasSubject) {
            books = repository.findByStatusAndTitleContainingIgnoreCaseAndSubject(status, keyword.trim(), subject);
        } else if (hasKeyword) {
            books = repository.findByStatusAndTitleContainingIgnoreCase(status, keyword.trim());
        } else if (hasSubject) {
            books = repository.findByStatusAndSubject(status, subject);
        } else {
            books = repository.findByStatus(status);
        }

        return books.stream()
                .filter(b -> isBlank(conditionGrade) || conditionGrade.equals(b.getConditionGrade()))
                .sorted(comparatorFor(sort))
                .toList();
    }

    // 看一本書。待審核、已退回的書還沒公開（US07），只有賣家本人和管理員看得到，其他人當作找不到。
    // viewer 是 null 代表沒登入
    public Book getBookById(String id, LoginUser viewer) {
        Book book = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + id));
        if (isHidden(book) && !canSeeHidden(book, viewer)) {
            throw new ResourceNotFoundException("Book not found with id " + id);
        }
        return book;
    }

    // 我的刊登（T06）：賣家自己所有狀態的書，最新刊登的在前；已退回的書帶著退回原因（US07）
    public List<Book> findMyBooks(LoginUser seller) {
        return repository.findBySellerId(seller.id()).stream()
                .sorted(NEWEST_FIRST)
                .toList();
    }

    // ===== US07 審核書籍（只有管理員能呼叫，權限在 SecurityConfig 的 /api/admin/** 檢查） =====

    // 待審核清單，先刊登的先審
    public List<Book> findPendingBooks() {
        return repository.findByStatus(Book.STATUS_PENDING).stream()
                .sorted(NEWEST_FIRST.reversed())
                .toList();
    }

    // 核准：待審核 → 上架中，買家就能在列表找到
    public Book approve(String id) {
        Book book = findPendingBook(id, "只有「待審核」的書可以核准");
        book.setStatus(Book.STATUS_ON_SALE);
        book.setRejectReason(null);
        book.setReviewedAt(LocalDateTime.now());
        return repository.save(book);
    }

    // 退回：待審核 → 已退回，一定要填原因（賣家在「我的刊登」看得到）；原因留白就擋下，狀態不變
    public Book reject(String id, String reason) {
        if (isBlank(reason)) {
            throw new InvalidBookException("請填寫退回原因");
        }
        Book book = findPendingBook(id, "只有「待審核」的書可以退回");
        book.setStatus(Book.STATUS_REJECTED);
        book.setRejectReason(reason.trim());
        book.setReviewedAt(LocalDateTime.now());
        return repository.save(book);
    }

    private Book findPendingBook(String id, String wrongStatusMessage) {
        Book book = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + id));
        if (!Book.STATUS_PENDING.equals(book.getStatus())) {
            throw new InvalidBookException(wrongStatusMessage + "（這本書目前是「" + book.getStatus() + "」）");
        }
        return book;
    }

    private static boolean isHidden(Book book) {
        return Book.STATUS_PENDING.equals(book.getStatus()) || Book.STATUS_REJECTED.equals(book.getStatus());
    }

    private static boolean canSeeHidden(Book book, LoginUser viewer) {
        return viewer != null && (viewer.admin() || viewer.id().equals(book.getSellerId()));
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
