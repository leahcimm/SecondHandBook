package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.controller.BookController;
import ntou.cs.secondhandbook.exception.InvalidBookException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 測試 API 的回應格式：成功 201、資料錯誤 400 + 中文訊息
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void 刊登成功回201() throws Exception {
        Book saved = new Book();
        saved.setTitle("資料結構");
        saved.setStatus("待審核");
        when(bookService.addBook(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(saved);

        mockMvc.perform(multipart("/api/books")
                        .file(new MockMultipartFile("image", "c.jpg", "image/jpeg", new byte[]{1}))
                        .param("title", "資料結構").param("subject", "資料結構").param("price", "250"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("待審核"));
    }

    @Test
    void 資料錯誤回400與訊息() throws Exception {
        when(bookService.addBook(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidBookException("售價必須為正整數"));

        mockMvc.perform(multipart("/api/books").param("price", "-50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("售價必須為正整數"));
    }

    @Test
    void 列表回傳陣列() throws Exception {
        when(bookService.findBooks(any(), any())).thenReturn(List.of(new Book(), new Book()));

        mockMvc.perform(get("/api/books").param("keyword", "資料"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
