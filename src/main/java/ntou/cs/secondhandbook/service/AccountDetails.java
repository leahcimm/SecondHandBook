package ntou.cs.secondhandbook.service;

import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Spring Security 存在 Session 裡的「登入者」。登入成功後密碼雜湊會被清掉（eraseCredentials），
// Session 裡只留 LoginUser（編號、姓名、Email、聯絡方式）。
// 管理員（T06）有 ROLE_ADMIN 權限，SecurityConfig 用它限制 /api/admin/**。
public class AccountDetails implements UserDetails, CredentialsContainer {

    private final LoginUser loginUser;
    private String passwordHash;

    public AccountDetails(LoginUser loginUser, String passwordHash) {
        this.loginUser = loginUser;
        this.passwordHash = passwordHash;
    }

    public static AccountDetails from(User user) {
        return new AccountDetails(LoginUser.from(user), user.getPasswordHash());
    }

    public LoginUser getLoginUser() {
        return loginUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return loginUser.admin() ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN")) : List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return loginUser.email();
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
