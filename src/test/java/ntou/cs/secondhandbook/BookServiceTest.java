package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.exception.InvalidBookException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.BookForm;
import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.repository.BookRepository;
import ntou.cs.secondhandbook.service.BookService;
import ntou.cs.secondhandbook.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 對應需求文件 US02 / US03 的測試方法（不需要 MongoDB 就能跑）
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private BookService bookService;

    private final MockMultipartFile jpg =
            new MockMultipartFile("image", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3});

    // 一份所有必填欄位都正確的表單，各測試再改掉要測的那一格
    private static final LoginUser SELLER = new LoginUser("uA", "阿明", "amin@mail.ntou.edu.tw", "IG amin");

    private BookForm validForm() {
        BookForm form = new BookForm();
        form.setTitle("資料結構");
        form.setSubject("資料結構");
        form.setConditionGrade("良好");
        form.setPrice("250");
        form.setImage(jpg);
        return form;
    }

    private void assertRejected(BookForm form, String expectedMessage) {
        InvalidBookException ex = assertThrows(InvalidBookException.class, () -> bookService.addBook(form, SELLER));
        assertEquals(expectedMessage, ex.getMessage());
        verifyNoInteractions(repository);
    }

    private static Book book(String title, int price, String grade, int daysAgo) {
        Book b = new Book();
        b.setTitle(title);
        b.setPrice(price);
        b.setConditionGrade(grade);
        b.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return b;
    }

    private static List<Integer> prices(List<Book> books) {
        return books.stream().map(Book::getPrice).toList();
    }

    // ===== US02 刊登書籍 =====

    @Test
    void 填寫所有欄位_刊登成功且狀態為待審核() throws Exception {
        when(fileStorageService.storeFile(any())).thenReturn("abc_cover.jpg");
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookForm form = validForm();
        form.setAuthor("Horowitz");
        form.setCondition("有少量螢光筆畫記，無缺頁");
        Book book = bookService.addBook(form, SELLER);

        assertEquals("資料結構", book.getTitle());
        assertEquals("良好", book.getConditionGrade());
        assertEquals("有少量螢光筆畫記，無缺頁", book.getCondition());
        assertEquals(250, book.getPrice());
        assertEquals("待審核", book.getStatus());
        assertEquals("/uploads/abc_cover.jpg", book.getImageUrl());
        assertNotNull(book.getCreatedAt());
        // T05：賣家資料從登入者帶入，不放 Email
        assertEquals("uA", book.getSellerId());
        assertEquals("阿明", book.getSellerName());
        assertEquals("IG amin", book.getSellerContact());
        verify(repository).save(any(Book.class));
    }

    @Test
    void 書名留白_拒絕刊登() {
        BookForm form = validForm();
        form.setTitle("  ");
        assertRejected(form, "請填寫書名");
    }

    @Test
    void 未選科目_拒絕刊登() {
        BookForm form = validForm();
        form.setSubject("");
        assertRejected(form, "請選擇科目");
    }

    @Test
    void 未選書況等級_拒絕刊登() {
        BookForm form = validForm();
        form.setConditionGrade(null);
        assertRejected(form, "請選擇書況");
    }

    @Test
    void 書況等級不在五級之內_拒絕刊登() {
        BookForm form = validForm();
        form.setConditionGrade("有點舊");
        assertRejected(form, "請選擇書況");
    }

    @Test
    void 售價為負數_文字_零_小數_都拒絕刊登() {
        for (String badPrice : new String[]{"-50", "abc", "0", "12.5"}) {
            BookForm form = validForm();
            form.setPrice(badPrice);
            InvalidBookException ex = assertThrows(InvalidBookException.class, () -> bookService.addBook(form, SELLER));
            assertEquals("售價必須為正整數", ex.getMessage(), "售價 " + badPrice);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void 沒有照片_拒絕刊登() {
        BookForm form = validForm();
        form.setImage(null);
        assertRejected(form, "請上傳書籍照片");
    }

    @Test
    void 照片格式不對_拒絕刊登() {
        BookForm form = validForm();
        form.setImage(new MockMultipartFile("image", "a.pdf", "application/pdf", new byte[]{1}));
        assertRejected(form, "照片格式只接受 JPG、PNG、WEBP");
    }

    @Test
    void 只能面交_不存運費() throws Exception {
        when(fileStorageService.storeFile(any())).thenReturn("abc_cover.jpg");
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookForm form = validForm();
        form.setShippingFee("60"); // 沒勾可寄送，填了運費也不算
        Book book = bookService.addBook(form, SELLER);

        assertFalse(book.isShippable());
        assertNull(book.getShippingFee());
    }

    @Test
    void 勾選寄送並填運費_刊登成功() throws Exception {
        when(fileStorageService.storeFile(any())).thenReturn("abc_cover.jpg");
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookForm form = validForm();
        form.setShippable(true);
        form.setShippingFee("60");
        Book book = bookService.addBook(form, SELLER);

        assertTrue(book.isShippable());
        assertEquals(60, book.getShippingFee());
    }

    @Test
    void 勾選寄送_運費填0也可以() throws Exception {
        when(fileStorageService.storeFile(any())).thenReturn("abc_cover.jpg");
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookForm form = validForm();
        form.setShippable(true);
        form.setShippingFee("0");

        assertEquals(0, bookService.addBook(form, SELLER).getShippingFee());
    }

    @Test
    void 勾選寄送但沒填運費_拒絕刊登() {
        BookForm form = validForm();
        form.setShippable(true);
        assertRejected(form, "勾選寄送時請填寫運費");
    }

    @Test
    void 運費為負數_文字_小數_都拒絕刊登() {
        for (String badFee : new String[]{"-1", "abc", "12.5"}) {
            BookForm form = validForm();
            form.setShippable(true);
            form.setShippingFee(badFee);
            InvalidBookException ex = assertThrows(InvalidBookException.class, () -> bookService.addBook(form, SELLER));
            assertEquals("運費必須為 0 以上的整數", ex.getMessage(), "運費 " + badFee);
        }
        verifyNoInteractions(repository);
    }

    // ===== US03 尋找書籍 =====

    @Test
    void 列表_沒有條件時列出全部() {
        bookService.findBooks(null, "", null, null);
        verify(repository).findAll();
    }

    @Test
    void 列表_用關鍵字和科目篩選() {
        bookService.findBooks(" 資料 ", "資料結構", null, null);
        verify(repository).findByTitleContainingIgnoreCaseAndSubject("資料", "資料結構");
    }

    @Test
    void 三位賣家賣同一本書_預設依價格由低到高() {
        when(repository.findByTitleContainingIgnoreCase("資料結構")).thenReturn(List.of(
                book("資料結構", 300, "全新", 1),
                book("資料結構", 200, "普通", 2),
                book("資料結構", 250, "良好", 3)));

        List<Book> result = bookService.findBooks("資料結構", null, null, null);

        assertEquals(List.of(200, 250, 300), prices(result));
    }

    @Test
    void 列表_依最新刊登排序() {
        when(repository.findAll()).thenReturn(List.of(
                book("A", 300, "全新", 3),
                book("B", 200, "普通", 1),
                book("C", 250, "良好", 2)));

        List<Book> result = bookService.findBooks(null, null, null, "newest");

        assertEquals(List.of("B", "C", "A"), result.stream().map(Book::getTitle).toList());
    }

    @Test
    void 列表_依書況排序_同等級再比價格_沒有等級的舊資料排最後() {
        when(repository.findAll()).thenReturn(List.of(
                book("舊資料", 100, null, 1),
                book("普通", 150, "普通", 1),
                book("良好貴", 300, "良好", 1),
                book("全新", 400, "全新", 1),
                book("良好便宜", 220, "良好", 1)));

        List<Book> result = bookService.findBooks(null, null, null, "condition");

        assertEquals(List.of("全新", "良好便宜", "良好貴", "普通", "舊資料"),
                result.stream().map(Book::getTitle).toList());
    }

    @Test
    void 列表_依書況等級篩選() {
        when(repository.findAll()).thenReturn(List.of(
                book("A", 300, "全新", 1),
                book("B", 200, "良好", 1),
                book("C", 250, "良好", 1)));

        List<Book> result = bookService.findBooks(null, null, "良好", null);

        assertEquals(List.of("B", "C"), result.stream().map(Book::getTitle).toList());
    }
}
