package ntou.cs.secondhandbook.service;

import ntou.cs.secondhandbook.exception.InvalidUserException;
import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.RegisterForm;
import ntou.cs.secondhandbook.model.User;
import ntou.cs.secondhandbook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

// US01 註冊帳號（FR-AC-01）。登入由 Spring Security 處理，見 config/SecurityConfig。
@Service
public class UserService {

    // 只收海大學生信箱（2026-11-17 Michael 決定）
    public static final String SCHOOL_EMAIL_DOMAIN = "@mail.ntou.edu.tw";

    static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt 只看前 72 個位元組，超過的部分會被忽略，所以直接擋下
    static final int MAX_PASSWORD_BYTES = 72;

    // 一般的 Email 格式：名稱@網域.後綴，不能有空白
    private static final Pattern EMAIL_FORMAT = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginUser register(RegisterForm form) {
        if (isBlank(form.getName())) {
            throw new InvalidUserException("請填寫姓名");
        }
        if (isBlank(form.getEmail())) {
            throw new InvalidUserException("請填寫 Email");
        }
        String email = normalizeEmail(form.getEmail());
        if (!EMAIL_FORMAT.matcher(email).matches()) {
            throw new InvalidUserException("Email 格式不正確");
        }
        if (!email.endsWith(SCHOOL_EMAIL_DOMAIN)) {
            throw new InvalidUserException("請使用學校 Email（" + SCHOOL_EMAIL_DOMAIN + "）註冊");
        }
        String password = form.getPassword() == null ? "" : form.getPassword();
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidUserException("密碼至少要 " + MIN_PASSWORD_LENGTH + " 個字元");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new InvalidUserException("密碼太長，請在 " + MAX_PASSWORD_BYTES + " 個英數字元以內");
        }
        if (isBlank(form.getContact())) {
            throw new InvalidUserException("請填寫聯絡方式");
        }
        if (userRepository.existsByEmail(email)) {
            throw new InvalidUserException("這個 Email 已經註冊過");
        }

        User user = new User();
        user.setName(form.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setContact(form.getContact().trim());
        user.setCreatedAt(LocalDateTime.now());
        try {
            return LoginUser.from(userRepository.save(user));
        } catch (DuplicateKeyException e) {
            // 兩個人幾乎同時用同一個 Email 註冊：資料庫的唯一索引會擋下第二個
            throw new InvalidUserException("這個 Email 已經註冊過");
        }
    }

    // Email 不分大小寫，前後空白也去掉；登入時也用同樣的方式比對
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
