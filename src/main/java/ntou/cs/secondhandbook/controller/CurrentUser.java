package ntou.cs.secondhandbook.controller;

import ntou.cs.secondhandbook.exception.MissingUserException;
import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.service.AccountDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// 「目前使用者是誰」只在這裡決定（PLAN 原則）。
// T05：從 Session 取登入者（Spring Security 登入成功後放進去的 AccountDetails）。
// 需要登入的 API 在 SecurityConfig 就會先擋下沒登入的請求；這裡再檢查一次，以防設定漏掉。
@Component
public class CurrentUser {

    public LoginUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AccountDetails details) {
            return details.getLoginUser();
        }
        throw new MissingUserException("請先登入");
    }
}
