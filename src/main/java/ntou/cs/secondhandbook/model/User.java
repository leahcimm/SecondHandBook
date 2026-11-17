package ntou.cs.secondhandbook.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// 使用者帳號（US01、FR-AC-01）。Email 就是登入帳號，只收 @mail.ntou.edu.tw（PLAN 技術決定）。
// 密碼只存 BCrypt 雜湊（NFR-03）。這個類別不直接回給前端，回給前端用 LoginUser（不含密碼）。
@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String name;          // 姓名，刊登書籍時當作賣家名稱
    @Indexed(unique = true)
    private String email;         // 學校 Email，存小寫
    private String passwordHash;  // BCrypt 雜湊，資料庫裡不會有明碼
    private String contact;       // 聯絡方式，刊登書籍時當作賣家聯絡方式
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
