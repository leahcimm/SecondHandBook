package ntou.cs.secondhandbook.model;

import java.io.Serializable;

// 目前登入的使用者：Controller 從 CurrentUser 取得，再交給 Service。
// 也是 /api/auth/me、註冊、登入成功時回給前端的資料，所以不含密碼。
// id 用來判斷誰是買家、賣家；name 只拿來顯示（姓名可能重複）。
// admin：是不是管理員（T06），前端用它決定導覽列要不要顯示「審核書籍」；真正的權限檢查在後端。
// 會跟著登入狀態存進 Session，所以要可以序列化（Session 存到硬碟時才不會出錯）。
public record LoginUser(String id, String name, String email, String contact, boolean admin) implements Serializable {

    // 一般使用者（不是管理員）
    public LoginUser(String id, String name, String email, String contact) {
        this(id, name, email, contact, false);
    }

    public static LoginUser from(User user) {
        return new LoginUser(user.getId(), user.getName(), user.getEmail(), user.getContact(), user.isAdmin());
    }
}
