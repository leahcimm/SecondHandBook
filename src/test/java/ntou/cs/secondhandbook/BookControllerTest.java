package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.config.SecurityConfig;
import ntou.cs.secondhandbook.controller.BookController;
import ntou.cs.secondhandbook.controller.CurrentUser;
import ntou.cs.secondhandbook.exception.InvalidBookException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.service.AccountDetails;
import ntou.cs.secondhandbook.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 測試 API 的回應格式：成功 201、資料錯誤 400 + 中文訊息。T05 起刊登要登入，列表不用
@WebMvcTest(BookController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class BookControllerTest {

    private static final LoginUser SELLER = new LoginUser("uA", "阿明", "ua@mail.ntou.edu.tw", "IG 阿明");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void 刊登成功回201() throws Exception {
        Book saved = new Book();
        saved.setTitle("資料結構");
        saved.setStatus("待審核");
        when(bookService.addBook(any(), any())).thenReturn(saved);

        mockMvc.perform(multipart("/api/books")
                        .file(new MockMultipartFile("image", "c.jpg", "image/jpeg", new byte[]{1}))
                        .with(user(new AccountDetails(SELLER, null)))
                        .param("title", "資料結構").param("subject", "資料結構").param("price", "250"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("待審核"));
    }

    @Test
    void 資料錯誤回400與訊息() throws Exception {
        when(bookService.addBook(any(), any()))
                .thenThrow(new InvalidBookException("售價必須為正整數"));

        mockMvc.perform(multipart("/api/books").with(user(new AccountDetails(SELLER, null))).param("price", "-50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("售價必須為正整數"));
    }

    @Test
    void 列表回傳陣列() throws Exception {
        when(bookService.findBooks(any(), any(), any(), any())).thenReturn(List.of(new Book(), new Book()));

        mockMvc.perform(get("/api/books").param("keyword", "資料"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 刊登表單欄位_含書況等級與寄送運費_都會傳給Service() throws Exception {
        when(bookService.addBook(any(), any())).thenReturn(new Book());

        mockMvc.perform(multipart("/api/books")
                        .file(new MockMultipartFile("image", "c.jpg", "image/jpeg", new byte[]{1}))
                        .with(user(new AccountDetails(SELLER, null)))
                        .param("title", "資料結構").param("subject", "資料結構").param("price", "250")
                        .param("conditionGrade", "良好").param("shippable", "true").param("shippingFee", "60"))
                .andExpect(status().isCreated());

        verify(bookService).addBook(argThat(form -> "良好".equals(form.getConditionGrade())
                && form.isShippable() && "60".equals(form.getShippingFee())
                && form.getImage() != null), eq(SELLER));
    }

    @Test
    void 列表的書況篩選與排序參數_會傳給Service() throws Exception {
        when(bookService.findBooks(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/books").param("conditionGrade", "良好").param("sort", "condition"))
                .andExpect(status().isOk());

        verify(bookService).findBooks(any(), any(), eq("良好"), eq("condition"));
    }

    @Test
    void 沒登入不能刊登_回401請先登入() throws Exception {
        mockMvc.perform(multipart("/api/books")
                        .file(new MockMultipartFile("image", "c.jpg", "image/jpeg", new byte[]{1}))
                        .param("title", "資料結構"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("請先登入"));
        verifyNoInteractions(bookService);
    }

    @Test
    void 沒登入也看得到列表() throws Exception {
        when(bookService.findBooks(any(), any(), any(), any())).thenReturn(List.of());
        mockMvc.perform(get("/api/books")).andExpect(status().isOk());
    }

    // ===== T06 =====

    @Test
    void 我的刊登要登入_沒登入回401() throws Exception {
        mockMvc.perform(get("/api/books/mine"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(bookService);
    }

    @Test
    void 我的刊登_用登入者查() throws Exception {
        when(bookService.findMyBooks(SELLER)).thenReturn(List.of(new Book()));
        mockMvc.perform(get("/api/books/mine").with(user(new AccountDetails(SELLER, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void 單本書_把登入者傳給Service判斷能不能看() throws Exception {
        when(bookService.getBookById(eq("b1"), eq(SELLER))).thenReturn(new Book());
        mockMvc.perform(get("/api/books/b1").with(user(new AccountDetails(SELLER, null))))
                .andExpect(status().isOk());
        when(bookService.getBookById(eq("b1"), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new ntou.cs.secondhandbook.exception.ResourceNotFoundException("x"));
        mockMvc.perform(get("/api/books/b1"))
                .andExpect(status().isNotFound());
    }
}
