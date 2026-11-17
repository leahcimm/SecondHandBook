package ntou.cs.secondhandbook.repository;

import ntou.cs.secondhandbook.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // 買家看自己送出的訂單、賣家看自己收到的訂單，新的在前
    List<Order> findByBuyerNameOrderByCreatedAtDesc(String buyerName);

    List<Order> findBySellerNameOrderByCreatedAtDesc(String sellerName);
}
