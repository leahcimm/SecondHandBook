package ntou.cs.secondhandbook.model;

import java.io.Serializable;

// 目前登入的使用者：Controller 從 CurrentUser 取得，再交給 Service。
// 也是 /api/auth/me、註冊、登入成功時回給前端的資料，所以不含密碼。
// id 用來判斷誰是買家、賣家；name 只拿來顯示（姓名可能重複）。
// 會跟著登入狀態存進 Session，所以要可以序列化（Session 存到硬碟時才不會出錯）。
public record LoginUser(String id, String name, String email, String contact) implements Serializable {

    public static LoginUser from(User user) {
        return new LoginUser(user.getId(), user.getName(), user.getEmail(), user.getContact());
    }
}
