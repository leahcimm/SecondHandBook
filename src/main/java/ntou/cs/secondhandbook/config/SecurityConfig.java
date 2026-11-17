package ntou.cs.secondhandbook.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import ntou.cs.secondhandbook.service.AccountDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.util.Map;

// T05 登入設定：Spring Security 內建表單登入 + Session（PLAN 技術決定）。
// 前端是 fetch 呼叫 API，所以登入成功、失敗、沒登入都回 JSON，不轉址到登入頁。
@Configuration
public class SecurityConfig {

    public static final String LOGIN_URL = "/api/auth/login";
    public static final String LOGOUT_URL = "/api/auth/logout";

    // NFR-03：密碼用 BCrypt 雜湊後才存
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                // CSRF 關掉，改靠 Session Cookie 的 SameSite=Lax（application.properties），2026-11-17 Michael 決定
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // T06：審核書籍只有管理員能用（沒登入 401、不是管理員 403）
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // T06：我的刊登要登入。要寫在下面的 /api/books/** 前面，不然會被當成不用登入
                        .requestMatchers(HttpMethod.GET, "/api/books/mine").authenticated()
                        // 不用登入：看書籍列表和照片、註冊、登入、登入頁顯示的展示用管理員帳號
                        .requestMatchers(HttpMethod.GET, "/api/books", "/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", LOGIN_URL).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/demo-admin").permitAll()
                        // 其他 API 都要登入：刊登、訂單、查自己是誰
                        .requestMatchers("/api/**").authenticated()
                        // 靜態頁面和 /uploads/** 不用登入；頁面要不要登入由前端呼叫 API 後判斷
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        // 登入頁是 static/login.html（T12）；設定它也關掉 Spring Security 自動產生的 /login 頁面
                        .loginPage("/login.html")
                        .loginProcessingUrl(LOGIN_URL)
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            AccountDetails details = (AccountDetails) authentication.getPrincipal();
                            writeJson(response, objectMapper, HttpServletResponse.SC_OK, details.getLoginUser());
                        })
                        .failureHandler((request, response, exception) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        Map.of("message", "Email 或密碼錯誤"))))
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_URL)
                        .logoutSuccessHandler((request, response, authentication) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_OK,
                                        Map.of("message", "已登出"))))
                // 沒登入就呼叫要登入的 API：回 401 JSON，前端 api() 收到後帶使用者去登入頁（T12）
                // 有登入但不是管理員卻呼叫審核 API：回 403 JSON（T06）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, exception) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        Map.of("message", "請先登入")))
                        .accessDeniedHandler((request, response, exception) ->
                                writeJson(response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                        Map.of("message", "只有管理員可以使用這個功能"))));
        return http.build();
    }

    private static void writeJson(HttpServletResponse response, ObjectMapper objectMapper, int status, Object body)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
