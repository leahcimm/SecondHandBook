package ntou.cs.secondhandbook.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "books") //每本刊登的書有哪些欄位
public class Book {

    public static final String STATUS_PENDING = "待審核";

    // 書況五級（由新到舊），刊登時必選一個；排序「依書況」也照這個順序
    public static final List<String> CONDITION_GRADES = List.of("全新", "近全新", "良好", "普通", "有缺損");

    @Id
    private String id;
    private String title;        // 書名（必填）
    private String author;       // 作者
    private String subject;      // 科目（必填）
    private String conditionGrade; // 書況等級（必選，CONDITION_GRADES 其中一個）
    private String condition;    // 書況描述（選填，補充等級以外的細節）
    private int price;           // 售價，正整數（必填）
    private boolean shippable;   // 是否可寄送；校內面交一律可以，不用另外存
    private Integer shippingFee; // 運費，0 以上的整數；不可寄送時為 null
    private String imageUrl;     // 照片網址（必填）
    private String sellerName;   // 賣家稱呼，Iteration 1 還沒有登入，先手動填
    private String sellerContact;// 賣家聯絡方式
    private String status;       // 刊登狀態，預設「待審核」（US07 審核功能再改變）
    private LocalDateTime createdAt;

    // Getters and setters for all fields

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getConditionGrade() {
        return conditionGrade;
    }

    public void setConditionGrade(String conditionGrade) {
        this.conditionGrade = conditionGrade;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public boolean isShippable() {
        return shippable;
    }

    public void setShippable(boolean shippable) {
        this.shippable = shippable;
    }

    public Integer getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(Integer shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerContact() {
        return sellerContact;
    }

    public void setSellerContact(String sellerContact) {
        this.sellerContact = sellerContact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
