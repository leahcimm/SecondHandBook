package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.config.SecurityConfig;
import ntou.cs.secondhandbook.controller.AdminController;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// US07 審核 API：只有管理員能用；退回原因留白回 400
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class AdminControllerTest {

    private static final LoginUser ADMIN = new LoginUser("admin", "系統管理員", "admin@mail.ntou.edu.tw", "平台管理員", true);
    private static final LoginUser NORMAL = new LoginUser("u1", "小美", "u1@mail.ntou.edu.tw", "IG 小美");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    private static Book book(String status) {
        Book b = new Book();
        b.setId("b1");
        b.setStatus(status);
        return b;
    }

    @Test
    void 管理員看待審核清單() throws Exception {
        when(bookService.findPendingBooks()).thenReturn(List.of(book("待審核")));
        mockMvc.perform(get("/api/admin/books").with(user(new AccountDetails(ADMIN, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("待審核"));
    }

    @Test
    void 管理員核准_回上架中() throws Exception {
        when(bookService.approve("b1")).thenReturn(book("上架中"));
        mockMvc.perform(post("/api/admin/books/b1/approve").with(user(new AccountDetails(ADMIN, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("上架中"));
    }

    @Test
    void 管理員退回_原因傳給Service() throws Exception {
        Book rejected = book("已退回");
        rejected.setRejectReason("書況描述不足");
        when(bookService.reject("b1", "書況描述不足")).thenReturn(rejected);
        mockMvc.perform(post("/api/admin/books/b1/reject").with(user(new AccountDetails(ADMIN, null)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"書況描述不足\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectReason").value("書況描述不足"));
    }

    @Test
    void 退回原因留白_回400要求補填() throws Exception {
        when(bookService.reject("b1", "")).thenThrow(new InvalidBookException("請填寫退回原因"));
        mockMvc.perform(post("/api/admin/books/b1/reject").with(user(new AccountDetails(ADMIN, null)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("請填寫退回原因"));
    }

    @Test
    void 不是管理員_回403() throws Exception {
        mockMvc.perform(post("/api/admin/books/b1/approve").with(user(new AccountDetails(NORMAL, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只有管理員可以使用這個功能"));
        mockMvc.perform(get("/api/admin/books").with(user(new AccountDetails(NORMAL, null))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(bookService);
    }

    @Test
    void 沒登入_回401() throws Exception {
        mockMvc.perform(post("/api/admin/books/b1/reject")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("請先登入"));
        verifyNoInteractions(bookService);
    }
}
