package ntou.cs.secondhandbook.model;

import java.util.List;

// 送出訂單時前端送來的 JSON：{"bookIds": ["...", "..."], "deliveryMethod": "面交"}
// 買家是誰由 CurrentUser 決定，金額由後端依資料庫裡的書價計算，前端不用送
public class OrderForm {

    private List<String> bookIds;
    private String deliveryMethod;

    public List<String> getBookIds() {
        return bookIds;
    }

    public void setBookIds(List<String> bookIds) {
        this.bookIds = bookIds;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
}
