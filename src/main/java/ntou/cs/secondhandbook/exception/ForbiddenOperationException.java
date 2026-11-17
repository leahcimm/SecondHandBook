package ntou.cs.secondhandbook.exception;

// 不是這筆訂單的賣家或買家，卻想操作訂單時丟出（回 403）
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
