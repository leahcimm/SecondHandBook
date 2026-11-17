package ntou.cs.secondhandbook.exception;

// 請求裡沒有帶目前使用者時丟出（回 401）。T05 做登入後改成「請先登入」
public class MissingUserException extends RuntimeException {
    public MissingUserException(String message) {
        super(message);
    }
}
