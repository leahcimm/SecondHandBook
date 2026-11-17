package ntou.cs.secondhandbook.repository;

import ntou.cs.secondhandbook.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {

    // Spring Data 會依方法名稱自動產生查詢，不用自己寫 SQL / Mongo 語法
    // 排序與書況篩選在 BookService 處理（書況要照等級順序排，資料庫只會照字排）
    // 列表只顯示某個狀態的書（T06：只顯示「上架中」），所以每個查詢都帶狀態
    List<Book> findByStatus(String status);

    List<Book> findByStatusAndTitleContainingIgnoreCase(String status, String keyword);

    List<Book> findByStatusAndSubject(String status, String subject);

    List<Book> findByStatusAndTitleContainingIgnoreCaseAndSubject(String status, String keyword, String subject);

    // 我的刊登（T06）：賣家自己所有狀態的書
    List<Book> findBySellerId(String sellerId);
}
