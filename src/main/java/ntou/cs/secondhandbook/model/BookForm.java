package ntou.cs.secondhandbook.model;

import org.springframework.web.multipart.MultipartFile;

// 刊登表單送進來的原始資料（都先當文字收，檢查交給 BookService）
// Spring 會依欄位名稱把 multipart/form-data 的同名欄位自動填進來
// 賣家資料不在表單裡：T05 起由登入者帶入（見 BookService.addBook）
public class BookForm {

    private String title;
    private String author;
    private String subject;
    private String conditionGrade;
    private String condition;
    private String price;
    private boolean shippable;   // 勾選「可寄送」時為 true
    private String shippingFee;
    private MultipartFile image;

    // Getters and setters for all fields

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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public boolean isShippable() {
        return shippable;
    }

    public void setShippable(boolean shippable) {
        this.shippable = shippable;
    }

    public String getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(String shippingFee) {
        this.shippingFee = shippingFee;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}
