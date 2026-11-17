package ntou.cs.secondhandbook.repository;

import ntou.cs.secondhandbook.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {

    // Spring Data 會依方法名稱自動產生查詢，不用自己寫 SQL / Mongo 語法
    // 排序與書況篩選在 BookService 處理（書況要照等級順序排，資料庫只會照字排）
    List<Book> findByTitleContainingIgnoreCase(String keyword);

    List<Book> findBySubject(String subject);

    List<Book> findByTitleContainingIgnoreCaseAndSubject(String keyword, String subject);

    // US09：查詢同書名、同作者、指定狀態的書籍
    List<Book> findByTitleAndAuthorAndStatus(String title, String author, String status);

    // US10：查詢賣家的所有刊登, 管理自己刊登的書籍(包括下架)
    List<Book> findBySellerName(String sellerName);
    
    // US07: 依照書籍狀態查詢
    List<Book> findByStatus(String status);
}
