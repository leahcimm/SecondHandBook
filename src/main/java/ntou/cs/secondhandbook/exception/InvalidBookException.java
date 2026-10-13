package ntou.cs.secondhandbook.exception;

// 刊登資料不符合規則時丟出，訊息會直接顯示給使用者
public class InvalidBookException extends RuntimeException {
    public InvalidBookException(String message) {
        super(message);
    }
}
