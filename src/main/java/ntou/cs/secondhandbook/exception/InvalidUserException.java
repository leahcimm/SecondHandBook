package ntou.cs.secondhandbook.exception;

// 註冊資料有誤（回 400），例如 Email 不是學校帳號、已經註冊過
public class InvalidUserException extends RuntimeException {
    public InvalidUserException(String message) {
        super(message);
    }
}
