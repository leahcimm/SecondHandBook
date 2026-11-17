package ntou.cs.secondhandbook.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 一筆訂單只對應一位賣家，可以有多本書（US04、FR-OD-02）
@Document(collection = "orders")
public class Order {

    // 訂單狀態，見需求文件第 6 章「訂單狀態」
    public static final String STATUS_WAITING_SELLER = "待賣家回覆";
    public static final String STATUS_TO_DELIVER = "待交付";
    public static final String STATUS_COMPLETED = "已完成";
    public static final String STATUS_REJECTED = "已拒絕";
    public static final String STATUS_CANCELLED = "已取消";

    // 交付方式
    public static final String DELIVERY_FACE_TO_FACE = "面交";
    public static final String DELIVERY_SHIPPING = "寄送";

    // 自動拒絕的原因開頭（T11）。賣家手動拒絕時 rejectReason 是 null，前端用這點分辨兩種拒絕
    public static final String AUTO_REJECT_REASON_PREFIX = "賣家已接受其他買家的訂單：";

    @Id
    private String id;
    private String buyerName;
    private String sellerName;
    private List<OrderItem> items = new ArrayList<>();
    private String deliveryMethod;  // 面交或寄送
    private int shippingFee;        // 面交為 0；寄送時一筆訂單只收一次，取這幾本書中最高的運費
    private int totalAmount;        // 書價合計 + shippingFee
    private String status;
    private String rejectReason;    // 自動拒絕時寫原因，例如「賣家已接受其他買家的訂單：《書名》」；手動拒絕是 null
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public int getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(int shippingFee) {
        this.shippingFee = shippingFee;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
