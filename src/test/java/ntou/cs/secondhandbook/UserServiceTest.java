package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.exception.InvalidUserException;
import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.RegisterForm;
import ntou.cs.secondhandbook.model.User;
import ntou.cs.secondhandbook.repository.UserRepository;
import ntou.cs.secondhandbook.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 對應需求文件 US01 的測試方法（後端部分）和 NFR-03（密碼雜湊）
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserService userService;

    private static RegisterForm form(String email) {
        RegisterForm f = new RegisterForm();
        f.setName("小美");
        f.setEmail(email);
        f.setPassword("secret123");
        f.setContact("IG xiaomei");
        return f;
    }

    private void assertRejected(RegisterForm form, String expectedMessage) {
        InvalidUserException ex = assertThrows(InvalidUserException.class, () -> userService.register(form));
        assertEquals(expectedMessage, ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void 未註冊過的學校Email_註冊成功() {
        when(userRepository.existsByEmail("01157009@mail.ntou.edu.tw")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("u1");
            return u;
        });

        LoginUser saved = userService.register(form("01157009@mail.ntou.edu.tw"));

        assertEquals("u1", saved.id());
        assertEquals("小美", saved.name());
        assertEquals("01157009@mail.ntou.edu.tw", saved.email());
        assertEquals("IG xiaomei", saved.contact());
    }

    @Test
    void 密碼用BCrypt雜湊後才存_資料庫看不到明碼() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.register(form("01157009@mail.ntou.edu.tw"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String hash = captor.getValue().getPasswordHash();
        assertNotEquals("secret123", hash);
        assertFalse(hash.contains("secret123"));
        assertTrue(hash.startsWith("$2"), "BCrypt 雜湊以 $2 開頭");
        assertTrue(passwordEncoder.matches("secret123", hash));
    }

    @Test
    void Email大寫和前後空白_存成小寫() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        LoginUser saved = userService.register(form("  AMin@Mail.NTOU.edu.tw "));
        assertEquals("amin@mail.ntou.edu.tw", saved.email());
    }

    @Test
    void 重複的帳號_註冊失敗() {
        when(userRepository.existsByEmail("01157009@mail.ntou.edu.tw")).thenReturn(true);
        assertRejected(form("01157009@mail.ntou.edu.tw"), "這個 Email 已經註冊過");
    }

    @Test
    void 兩人同時註冊同一個Email_資料庫擋下第二個_也顯示已註冊過() {
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("dup"));
        InvalidUserException ex = assertThrows(InvalidUserException.class,
                () -> userService.register(form("01157009@mail.ntou.edu.tw")));
        assertEquals("這個 Email 已經註冊過", ex.getMessage());
    }

    @Test
    void 非學校帳號_註冊失敗() {
        assertRejected(form("xiaomei@gmail.com"), "請使用學校 Email（@mail.ntou.edu.tw）註冊");
        // 老師、職員的信箱也不收（2026-11-17 Michael 決定只收學生信箱）
        assertRejected(form("teacher@ntou.edu.tw"), "請使用學校 Email（@mail.ntou.edu.tw）註冊");
        // 網域只是長得像也不行
        assertRejected(form("x@mail.ntou.edu.tw.evil.com"), "請使用學校 Email（@mail.ntou.edu.tw）註冊");
    }

    @Test
    void Email格式不正確_註冊失敗() {
        assertRejected(form("01157009"), "Email 格式不正確");
        assertRejected(form("0115 7009@mail.ntou.edu.tw"), "Email 格式不正確");
        assertRejected(form("@mail.ntou.edu.tw"), "Email 格式不正確");
    }

    @Test
    void 必填欄位留白_註冊失敗() {
        RegisterForm noName = form("a@mail.ntou.edu.tw");
        noName.setName(" ");
        assertRejected(noName, "請填寫姓名");

        assertRejected(form(""), "請填寫 Email");

        RegisterForm noContact = form("a@mail.ntou.edu.tw");
        noContact.setContact(null);
        assertRejected(noContact, "請填寫聯絡方式");
    }

    @Test
    void 密碼太短或太長_註冊失敗() {
        RegisterForm shortPassword = form("a@mail.ntou.edu.tw");
        shortPassword.setPassword("1234567");
        assertRejected(shortPassword, "密碼至少要 8 個字元");

        RegisterForm longPassword = form("a@mail.ntou.edu.tw");
        longPassword.setPassword("a".repeat(73));
        assertRejected(longPassword, "密碼太長，請在 72 個英數字元以內");
    }

    // ===== T06 管理員帳號 =====

    @Test
    void 註冊的帳號都不是管理員() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginUser user = userService.register(form("01157009@mail.ntou.edu.tw"));

        assertFalse(user.admin());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertNull(saved.getValue().getRole());
    }

    @Test
    void 啟動時沒有管理員帳號_建立一個() {
        when(userRepository.findByEmail("admin@mail.ntou.edu.tw")).thenReturn(java.util.Optional.empty());

        assertTrue(userService.ensureAdmin("Admin@mail.ntou.edu.tw", "admin12345", "系統管理員", "平台管理員"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User admin = saved.getValue();
        assertEquals("admin@mail.ntou.edu.tw", admin.getEmail());
        assertTrue(admin.isAdmin());
        assertTrue(passwordEncoder.matches("admin12345", admin.getPasswordHash()));
        assertTrue(LoginUser.from(admin).admin());
    }

    @Test
    void 啟動時已有這個Email_改成管理員_密碼改成設定檔的() {
        User existing = new User();
        existing.setId("u9");
        existing.setEmail("admin@mail.ntou.edu.tw");
        existing.setPasswordHash(passwordEncoder.encode("someoneElse1"));
        when(userRepository.findByEmail("admin@mail.ntou.edu.tw")).thenReturn(java.util.Optional.of(existing));

        assertFalse(userService.ensureAdmin("admin@mail.ntou.edu.tw", "admin12345", "系統管理員", "平台管理員"));

        assertTrue(existing.isAdmin());
        assertTrue(passwordEncoder.matches("admin12345", existing.getPasswordHash()));
        verify(userRepository).save(existing);
    }
}
