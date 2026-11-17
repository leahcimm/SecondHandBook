package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.config.SecurityConfig;
import ntou.cs.secondhandbook.controller.AuthController;
import ntou.cs.secondhandbook.controller.CurrentUser;
import ntou.cs.secondhandbook.exception.InvalidUserException;
import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.User;
import ntou.cs.secondhandbook.repository.UserRepository;
import ntou.cs.secondhandbook.service.AccountDetailsService;
import ntou.cs.secondhandbook.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// US01 註冊、登入、登出的 API。登入走真的 Spring Security 流程（BCrypt 比對、存進 Session），只有資料庫是假的
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CurrentUser.class, AccountDetailsService.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    private User saved;

    @BeforeEach
    void oneRegisteredUser() {
        saved = new User();
        saved.setId("u1");
        saved.setName("小美");
        saved.setEmail("01157009@mail.ntou.edu.tw");
        saved.setPasswordHash(passwordEncoder.encode("secret123"));
        saved.setContact("IG xiaomei");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("01157009@mail.ntou.edu.tw")).thenReturn(Optional.of(saved));
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email).param("password", password))
                .andReturn();
    }

    @Test
    void 註冊成功回201_不含密碼() throws Exception {
        when(userService.register(any()))
                .thenReturn(new LoginUser("u1", "小美", "01157009@mail.ntou.edu.tw", "IG xiaomei"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"小美\",\"email\":\"01157009@mail.ntou.edu.tw\",\"password\":\"secret123\",\"contact\":\"IG xiaomei\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("01157009@mail.ntou.edu.tw"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void 註冊失敗回400與原因_不用登入就能呼叫() throws Exception {
        when(userService.register(any())).thenThrow(new InvalidUserException("這個 Email 已經註冊過"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("這個 Email 已經註冊過"));
    }

    @Test
    void 帳密正確_登入成功回200和登入者_不含密碼雜湊() throws Exception {
        MvcResult result = login("01157009@mail.ntou.edu.tw", "secret123");
        org.junit.jupiter.api.Assertions.assertEquals(200, result.getResponse().getStatus());
        String body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"name\":\"小美\""), body);
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("$2"), "回應不能有 BCrypt 雜湊：" + body);
        org.junit.jupiter.api.Assertions.assertFalse(body.toLowerCase().contains("password"), body);
    }

    @Test
    void 登入後同一個Session_查me拿到自己_登出後回401() throws Exception {
        MockHttpSession session = (MockHttpSession) login("01157009@mail.ntou.edu.tw", "secret123")
                .getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.name").value("小美"))
                .andExpect(content().string(not(containsString("$2"))));

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已登出"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void Email大小寫不同也能登入() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(200,
                login("01157009@MAIL.ntou.edu.tw", "secret123").getResponse().getStatus());
    }

    @Test
    void 密碼錯誤或帳號不存在_回401_訊息一樣() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "01157009@mail.ntou.edu.tw").param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email 或密碼錯誤"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "nobody@mail.ntou.edu.tw").param("password", "secret123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email 或密碼錯誤"));
    }

    @Test
    void 沒登入查me_回401請先登入_不轉址() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("請先登入"));
    }
}
