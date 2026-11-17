package ntou.cs.secondhandbook.exception;

// 找不到訂單（回 404）。和找不到書分開，錯誤訊息才不會寫成「找不到這本書」
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
