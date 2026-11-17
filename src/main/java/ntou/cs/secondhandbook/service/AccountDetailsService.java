package ntou.cs.secondhandbook.service;

import ntou.cs.secondhandbook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// 登入時 Spring Security 用 Email 找帳號，再用 BCrypt 比對密碼（比對由 Spring Security 做）
@Service
public class AccountDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(UserService.normalizeEmail(email))
                .map(AccountDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("找不到這個帳號"));
    }
}
