package ntou.cs.secondhandbook.controller;

import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.RegisterForm;
import ntou.cs.secondhandbook.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// US01 帳號 API。
//   POST /api/auth/register  JSON {"name","email","password","contact"}，成功回 201（不會自動登入，前端跳到登入頁）
//   GET  /api/auth/me        回目前登入者，沒登入回 401
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
}
