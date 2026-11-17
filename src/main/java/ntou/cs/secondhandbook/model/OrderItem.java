package ntou.cs.secondhandbook.model;

// 訂單裡的一本書。下單當下把書名、價格、運費記下來，之後賣家改書價也不影響這筆訂單
public class OrderItem {

    private String bookId;
    private String title;
    private int price;
    private Integer shippingFee;      // 這本書的運費；不可寄送時為 null
    private String bookStatusBefore;  // 賣家接受前書的狀態；取消時還原成這個（例如「待審核」不會被跳過）

    public OrderItem() {
    }

    public OrderItem(Book book) {
        this.bookId = book.getId();
        this.title = book.getTitle();
        this.price = book.getPrice();
        this.shippingFee = book.getShippingFee();
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public Integer getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(Integer shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getBookStatusBefore() {
        return bookStatusBefore;
    }

    public void setBookStatusBefore(String bookStatusBefore) {
        this.bookStatusBefore = bookStatusBefore;
    }
}
