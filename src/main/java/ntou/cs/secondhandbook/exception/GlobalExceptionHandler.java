package ntou.cs.secondhandbook.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

// 全局異常處理器：把錯誤轉成 {"message": "..."}，前端直接顯示
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 刊登資料有誤：回 400 和中文錯誤訊息
    @ExceptionHandler(InvalidBookException.class)
    public ResponseEntity<Map<String, String>> handleInvalidBook(InvalidBookException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    // 照片超過 10MB（在進到 Controller 之前就會被擋下，所以要放在這裡處理）
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "照片不可超過 10MB"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("message", "找不到這本書"));
    }
}
