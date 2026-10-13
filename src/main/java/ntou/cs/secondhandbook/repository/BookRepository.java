package ntou.cs.secondhandbook.repository;

import ntou.cs.secondhandbook.model.Book;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {

    // Spring Data 會依方法名稱自動產生查詢，不用自己寫 SQL / Mongo 語法
    List<Book> findByTitleContainingIgnoreCase(String keyword, Sort sort);

    List<Book> findBySubject(String subject, Sort sort);

    List<Book> findByTitleContainingIgnoreCaseAndSubject(String keyword, String subject, Sort sort);
}
