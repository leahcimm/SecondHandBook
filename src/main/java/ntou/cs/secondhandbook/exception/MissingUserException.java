package ntou.cs.secondhandbook.exception;

// 需要登入的地方卻沒有登入時丟出（回 401「請先登入」）
public class MissingUserException extends RuntimeException {
    public MissingUserException(String message) {
        super(message);
    }
}
