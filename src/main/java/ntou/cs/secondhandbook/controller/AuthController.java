package ntou.cs.secondhandbook.controller;

import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.RegisterForm;
import ntou.cs.secondhandbook.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// US01 帳號 API。
//   POST /api/auth/register  JSON {"name","email","password","contact"}，成功回 201（不會自動登入，前端跳到登入頁）
//   GET  /api/auth/me        回目前登入者（含 admin 是不是管理員），沒登入回 401
//   GET  /api/auth/demo-admin 展示用管理員帳號 {"email","password"}，登入頁顯示（T06，Michael 決定）；
//                            application.properties 的 admin.show-on-login=false 時回 404
// 登入、登出由 Spring Security 處理（見 config/SecurityConfig）：
//   POST /api/auth/login     表單欄位 email、password（application/x-www-form-urlencoded），成功回 200 和登入者
//   POST /api/auth/logout    回 200
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/register")
    public ResponseEntity<LoginUser> register(@RequestBody RegisterForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(form));
    }

    @GetMapping("/me")
    public LoginUser me() {
        return currentUser.get();
    }

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.show-on-login:false}")
    private boolean showAdminOnLogin;

    // 課程展示用：登入頁直接顯示管理員帳號密碼，組員測試審核功能最方便（2026-11-17 Michael 決定）。
    // 正式上線前要把 admin.show-on-login 改成 false，並換掉 admin.password
    @GetMapping("/demo-admin")
    public ResponseEntity<Map<String, String>> demoAdmin() {
        if (!showAdminOnLogin) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("email", adminEmail, "password", adminPassword));
    }
}
