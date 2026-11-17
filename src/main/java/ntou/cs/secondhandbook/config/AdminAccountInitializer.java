package ntou.cs.secondhandbook.config;

import ntou.cs.secondhandbook.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// T06：程式啟動時建立管理員帳號（2026-11-17 Michael 決定）。帳號密碼在 application.properties 的 admin.*。
// 管理員不能自己註冊，註冊的帳號都是一般使用者。
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserService userService;

    @Value("${admin.email}")
    private String email;

    @Value("${admin.password}")
    private String password;

    @Value("${admin.name}")
    private String name;

    @Value("${admin.contact}")
    private String contact;

    public AdminAccountInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean created = userService.ensureAdmin(email, password, name, contact);
        log.info(created ? "已建立管理員帳號 {}" : "管理員帳號 {} 已存在，已確認角色和密碼", email);
    }
}
