package ntou.cs.secondhandbook.repository;

import ntou.cs.secondhandbook.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // 買家看自己送出的訂單、賣家看自己收到的訂單，新的在前
    // 用使用者編號查（T05），姓名可能重複
    List<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    List<Order> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    // 某個狀態、而且含有其中任一本書的訂單（查 items.bookId）。T11 接受訂單時用來找同一本書的其他訂單
    List<Order> findByStatusAndItemsBookIdIn(String status, Collection<String> bookIds);
}
