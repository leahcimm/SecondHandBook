package ntou.cs.secondhandbook.exception;

// 訂單資料不對，或訂單目前的狀態不能做這個動作時丟出（回 400），訊息會直接顯示給使用者
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
