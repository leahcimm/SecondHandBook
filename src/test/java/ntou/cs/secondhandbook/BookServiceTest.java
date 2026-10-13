package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.exception.InvalidBookException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.repository.BookRepository;
import ntou.cs.secondhandbook.service.BookService;
import ntou.cs.secondhandbook.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void 填寫所有欄位_刊登成功且狀態為待審核() throws Exception {
        when(fileStorageService.storeFile(any())).thenReturn("abc_cover.jpg");
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book book = bookService.addBook("資料結構", "Horowitz", "資料結構", "少量畫記", "250",
                "阿明", "amin@mail.ntou.edu.tw", jpg);

        assertEquals("資料結構", book.getTitle());
        assertEquals(250, book.getPrice());
        assertEquals("待審核", book.getStatus());
        assertEquals("/uploads/abc_cover.jpg", book.getImageUrl());
        assertNotNull(book.getCreatedAt());
        verify(repository).save(any(Book.class));
    }

    @Test
    void 書名留白_拒絕刊登() {
        InvalidBookException ex = assertThrows(InvalidBookException.class, () ->
                bookService.addBook("  ", null, "資料結構", null, "250", null, null, jpg));
        assertEquals("請填寫書名", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void 未選科目_拒絕刊登() {
        InvalidBookException ex = assertThrows(InvalidBookException.class, () ->
                bookService.addBook("資料結構", null, "", null, "250", null, null, jpg));
        assertEquals("請選擇科目", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void 售價為負數_文字_零_小數_都拒絕刊登() {
        for (String badPrice : new String[]{"-50", "abc", "0", "12.5"}) {
            InvalidBookException ex = assertThrows(InvalidBookException.class, () ->
                    bookService.addBook("資料結構", null, "資料結構", null, badPrice, null, null, jpg));
            assertEquals("售價必須為正整數", ex.getMessage(), "售價 " + badPrice);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void 沒有照片_拒絕刊登() {
        InvalidBookException ex = assertThrows(InvalidBookException.class, () ->
                bookService.addBook("資料結構", null, "資料結構", null, "250", null, null, null));
        assertEquals("請上傳書籍照片", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void 照片格式不對_拒絕刊登() {
        MockMultipartFile pdf = new MockMultipartFile("image", "a.pdf", "application/pdf", new byte[]{1});
        InvalidBookException ex = assertThrows(InvalidBookException.class, () ->
                bookService.addBook("資料結構", null, "資料結構", null, "250", null, null, pdf));
        assertEquals("照片格式只接受 JPG、PNG、WEBP", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void 列表_沒有條件時列出全部() {
        bookService.findBooks(null, "");
        verify(repository).findAll(any(Sort.class));
    }

    @Test
    void 列表_用關鍵字和科目篩選() {
        bookService.findBooks(" 資料 ", "資料結構");
        verify(repository).findByTitleContainingIgnoreCaseAndSubject(eq("資料"), eq("資料結構"), any(Sort.class));
    }
}
