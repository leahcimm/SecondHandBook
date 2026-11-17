package ntou.cs.secondhandbook.controller;

import jakarta.servlet.http.HttpServletRequest;
import ntou.cs.secondhandbook.exception.MissingUserException;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

// 「目前使用者是誰」只在這裡決定（PLAN 原則）。
// 暫代方式（T03）：前端在請求標頭 X-User-Name 帶使用者名稱，要和刊登書籍時填的 sellerName 一樣。
// 標頭不能直接放中文，前端要先用 encodeURIComponent 編碼，這裡再解碼。
// T05 做登入後，只要把這個方法改成從 Session 取登入者，Controller 和 Service 都不用改。
@Component
public class CurrentUser {

    public static final String HEADER = "X-User-Name";

    public String name(HttpServletRequest request) {
        String raw = request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            throw new MissingUserException("請先輸入你的名稱");
        }
        String name = URLDecoder.decode(raw, StandardCharsets.UTF_8).trim();
        if (name.isEmpty()) {
            throw new MissingUserException("請先輸入你的名稱");
        }
        return name;
    }
}
