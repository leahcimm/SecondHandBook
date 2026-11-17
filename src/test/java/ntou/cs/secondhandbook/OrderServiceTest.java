package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.exception.ForbiddenOperationException;
import ntou.cs.secondhandbook.exception.InvalidOrderException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.Order;
import ntou.cs.secondhandbook.model.OrderForm;
import ntou.cs.secondhandbook.repository.BookRepository;
import ntou.cs.secondhandbook.repository.OrderRepository;
import ntou.cs.secondhandbook.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// 對應需求文件 US04、US05、US11 的測試方法（不需要 MongoDB 就能跑）。
// 用兩個 Map 假裝是資料庫：存進去的書和訂單都可以再讀出來，才測得到「狀態一起變」。
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private OrderService orderService;

    private final Map<String, Book> bookDb = new HashMap<>();
    private final Map<String, Order> orderDb = new HashMap<>();
    private int nextOrderId = 1;

    // T05：用登入者判斷身分，比對的是使用者編號，不是姓名
    private static final LoginUser SELLER_A = user("uA", "阿明");
    private static final LoginUser SELLER_B = user("uB", "阿華");
    private static final LoginUser BUYER = user("u1", "小美");
    private static final LoginUser OTHER_BUYER = user("u2", "小強");

    private static LoginUser user(String id, String name) {
        return new LoginUser(id, name, id + "@mail.ntou.edu.tw", "IG " + name);
    }

    @BeforeEach
    void fakeDatabase() {
        lenient().when(bookRepository.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(bookDb.get(inv.<String>getArgument(0))));
        lenient().when(bookRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<Book> books = inv.getArgument(0);
            books.forEach(b -> bookDb.put(b.getId(), b));
            return books;
        });
        lenient().when(orderRepository.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(orderDb.get(inv.<String>getArgument(0))));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId("o" + nextOrderId++);
            }
            orderDb.put(o.getId(), o);
            return o;
        });
        // T11：找某個狀態、含有其中任一本書的訂單（真的資料庫查 items.bookId）
        lenient().when(orderRepository.findByStatusAndItemsBookIdIn(anyString(), any())).thenAnswer(inv -> {
            String status = inv.getArgument(0);
            java.util.Collection<String> ids = inv.getArgument(1);
            return orderDb.values().stream()
                    .filter(o -> status.equals(o.getStatus()))
                    .filter(o -> o.getItems().stream().anyMatch(item -> ids.contains(item.getBookId())))
                    .toList();
        });
    }

    // 建一本書放進假資料庫；shippingFee 給 null 代表不可寄送
    private Book book(String id, LoginUser seller, int price, Integer shippingFee) {
        Book b = new Book();
        b.setId(id);
        b.setTitle("書" + id);
        if (seller != null) {
            b.setSellerId(seller.id());
            b.setSellerName(seller.name());
        }
        b.setPrice(price);
        b.setShippable(shippingFee != null);
        b.setShippingFee(shippingFee);
        b.setStatus(Book.STATUS_ON_SALE);
        bookDb.put(id, b);
        return b;
    }

    private static OrderForm form(String delivery, String... bookIds) {
        OrderForm f = new OrderForm();
        f.setBookIds(List.of(bookIds));
        f.setDeliveryMethod(delivery);
        return f;
    }

    private String bookStatus(String id) {
        return bookDb.get(id).getStatus();
    }

    // 小美向阿明訂一本書 a1（200 元），回傳訂單編號
    private String pendingOrder() {
        book("a1", SELLER_A, 200, 60);
        return orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();
    }

    private String acceptedOrder() {
        String id = pendingOrder();
        orderService.accept(SELLER_A, id);
        return id;
    }

    // ===== US04 送出訂單 =====

    @Test
    void A的兩本書200與150_送出後只有一筆A的訂單_兩本書_總額350() {
        book("a1", SELLER_A, 200, null);
        book("a2", SELLER_A, 150, null);

        Order order = orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1", "a2"));

        assertEquals(1, orderDb.size());
        assertEquals(SELLER_A.id(), order.getSellerId());
        assertEquals("阿明", order.getSellerName());
        assertEquals(BUYER.id(), order.getBuyerId());
        assertEquals("小美", order.getBuyerName());
        assertEquals(2, order.getItems().size());
        assertEquals(350, order.getTotalAmount());
        assertEquals(Order.STATUS_WAITING_SELLER, order.getStatus());
    }

    @Test
    void 購物車有A和B的書_選A送出_訂單只含A的書_B的書不受影響() {
        book("a1", SELLER_A, 200, null);
        book("b1", SELLER_B, 300, null);

        Order order = orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1"));

        assertEquals(List.of("a1"), order.getItems().stream().map(i -> i.getBookId()).toList());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("b1"));
    }

    @Test
    void 一筆訂單混了兩位賣家的書_拒絕建立() {
        book("a1", SELLER_A, 200, null);
        book("b1", SELLER_B, 300, null);

        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1", "b1")));
        assertEquals("一筆訂單只能包含同一位賣家的書，請分開送出", ex.getMessage());
        assertTrue(orderDb.isEmpty());
    }

    @Test
    void 空購物車送出_提示沒有書籍_不建立訂單() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE)));
        assertEquals("購物車沒有書籍，請先加入書籍", ex.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void 送出失敗時_不建立訂單_書的狀態也不變() {
        // 寄送失敗的例子：a2 不可寄送
        book("a1", SELLER_A, 200, 60);
        book("a2", SELLER_A, 150, null);

        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(BUYER, form(Order.DELIVERY_SHIPPING, "a1", "a2")));
        assertEquals("《書a2》不提供寄送，請改選面交", ex.getMessage());
        verify(orderRepository, never()).save(any());
        verify(bookRepository, never()).saveAll(any());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("a1"));
    }

    @Test
    void 書價200選寄送運費60_總額260_改選面交_總額200() {
        book("a1", SELLER_A, 200, 60);

        Order shipped = orderService.createOrder(BUYER, form(Order.DELIVERY_SHIPPING, "a1"));
        Order faceToFace = orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1"));

        assertEquals(60, shipped.getShippingFee());
        assertEquals(260, shipped.getTotalAmount());
        assertEquals(0, faceToFace.getShippingFee());
        assertEquals(200, faceToFace.getTotalAmount());
    }

    @Test
    void 多本書寄送_運費只收一次_取最高的那本() {
        book("a1", SELLER_A, 200, 60);
        book("a2", SELLER_A, 150, 40);

        Order order = orderService.createOrder(BUYER, form(Order.DELIVERY_SHIPPING, "a1", "a2"));

        assertEquals(60, order.getShippingFee());
        assertEquals(410, order.getTotalAmount());
    }

    @Test
    void 沒選交付方式_拒絕建立() {
        book("a1", SELLER_A, 200, null);
        assertThrows(InvalidOrderException.class, () -> orderService.createOrder(BUYER, form(null, "a1")));
        assertTrue(orderDb.isEmpty());
    }

    @Test
    void 不能買自己刊登的書() {
        book("a1", SELLER_A, 200, null);
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(SELLER_A, form(Order.DELIVERY_FACE_TO_FACE, "a1")));
        assertEquals("不能購買自己刊登的書", ex.getMessage());
    }

    @Test
    void 買家和賣家同名但不同帳號_可以下單_權限照帳號判斷() {
        LoginUser sameNameBuyer = user("u9", "阿明");
        book("a1", SELLER_A, 200, null);
        String id = orderService.createOrder(sameNameBuyer, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();

        // 同名的買家不能用賣家的身分接受訂單
        assertThrows(ForbiddenOperationException.class, () -> orderService.accept(sameNameBuyer, id));
        assertEquals(Order.STATUS_TO_DELIVER, orderService.accept(SELLER_A, id).getStatus());
    }

    @Test
    void 沒有賣家帳號的書_不能下單() {
        book("a1", null, 200, null);
        assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")));
    }

    // T06：只有管理員核准過的「上架中」書能下單
    @Test
    void 待審核的書_不能下單() {
        book("a1", SELLER_A, 200, null).setStatus(Book.STATUS_PENDING);
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")));
        assertEquals("《書a1》還沒通過審核，不能下單", ex.getMessage());
        assertTrue(orderDb.isEmpty());
    }

    @Test
    void 已退回的書_不能下單() {
        book("a1", SELLER_A, 200, null).setStatus(Book.STATUS_REJECTED);
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")));
        assertEquals("《書a1》還沒通過審核，不能下單", ex.getMessage());
        assertTrue(orderDb.isEmpty());
    }

    // ===== US05 處理訂單 =====

    @Test
    void 賣家接受訂單後_訂單待交付_書保留中_其他買家不能再下單() {
        String id = pendingOrder();

        Order order = orderService.accept(SELLER_A, id);

        assertEquals(Order.STATUS_TO_DELIVER, order.getStatus());
        assertEquals(Book.STATUS_RESERVED, bookStatus("a1"));
        verify(bookRepository).saveAll(any()); // 書的新狀態有存回資料庫
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(OTHER_BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")));
        assertEquals("《書a1》已被保留或售出，不能下單", ex.getMessage());
    }

    @Test
    void 賣家拒絕訂單後_訂單已拒絕_書仍上架中且可再下單() {
        String id = pendingOrder();

        Order order = orderService.reject(SELLER_A, id);

        assertEquals(Order.STATUS_REJECTED, order.getStatus());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("a1"));
        Order again = orderService.createOrder(OTHER_BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1"));
        assertEquals(Order.STATUS_WAITING_SELLER, again.getStatus());
    }

    @Test
    void 非該訂單的賣家嘗試接受_拒絕操作_訂單狀態不變() {
        String id = pendingOrder();

        assertThrows(ForbiddenOperationException.class, () -> orderService.accept(SELLER_B, id));

        assertEquals(Order.STATUS_WAITING_SELLER, orderDb.get(id).getStatus());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("a1"));
        verify(bookRepository, never()).saveAll(any());
    }

    @Test
    void 非該訂單的賣家嘗試拒絕_拒絕操作_訂單狀態不變() {
        String id = pendingOrder();

        assertThrows(ForbiddenOperationException.class, () -> orderService.reject(SELLER_B, id));
        // 買家也不能拒絕
        assertThrows(ForbiddenOperationException.class, () -> orderService.reject(BUYER, id));

        assertEquals(Order.STATUS_WAITING_SELLER, orderDb.get(id).getStatus());
    }

    // ===== T11 接受一筆後，同一本書的其他待回覆訂單自動拒絕 =====

    @Test
    void 同一本書有兩筆待回覆訂單_接受第一筆後_第二筆自動已拒絕_第一筆待交付_書保留中() {
        book("a1", SELLER_A, 200, null);
        String first = orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();
        String second = orderService.createOrder(OTHER_BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();

        orderService.accept(SELLER_A, first);

        assertEquals(Order.STATUS_TO_DELIVER, orderDb.get(first).getStatus());
        assertNull(orderDb.get(first).getRejectReason());
        assertEquals(Order.STATUS_REJECTED, orderDb.get(second).getStatus());
        assertEquals("賣家已接受其他買家的訂單：《書a1》", orderDb.get(second).getRejectReason());
        assertEquals(Book.STATUS_RESERVED, bookStatus("a1"));
    }

    @Test
    void 賣家手動拒絕_沒有拒絕原因_和自動拒絕分得出來() {
        String id = pendingOrder();

        Order order = orderService.reject(SELLER_A, id);

        assertEquals(Order.STATUS_REJECTED, order.getStatus());
        assertNull(order.getRejectReason());
    }

    @Test
    void 自動拒絕時_不相干的訂單不受影響() {
        book("a1", SELLER_A, 200, null);
        book("a2", SELLER_A, 150, null);
        book("b1", SELLER_B, 100, null);
        String first = orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();
        String sameSellerOtherBook = orderService.createOrder(OTHER_BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a2")).getId();
        String otherSeller = orderService.createOrder(OTHER_BUYER, form(Order.DELIVERY_FACE_TO_FACE, "b1")).getId();

        orderService.accept(SELLER_A, first);

        assertEquals(Order.STATUS_WAITING_SELLER, orderDb.get(sameSellerOtherBook).getStatus());
        assertEquals(Order.STATUS_WAITING_SELLER, orderDb.get(otherSeller).getStatus());
        assertNull(orderDb.get(sameSellerOtherBook).getRejectReason());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("a2"));
        assertEquals(Book.STATUS_ON_SALE, bookStatus("b1"));
    }

    @Test
    void 另一筆訂單只有部分書被保留_也整筆拒絕_原因只列被保留的書() {
        book("a1", SELLER_A, 200, null);
        book("a2", SELLER_A, 150, null);
        String first = orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();
        String second = orderService.createOrder(OTHER_BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1", "a2")).getId();

        orderService.accept(SELLER_A, first);

        assertEquals(Order.STATUS_REJECTED, orderDb.get(second).getStatus());
        assertEquals("賣家已接受其他買家的訂單：《書a1》", orderDb.get(second).getRejectReason());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("a2")); // 沒被保留的書不動
    }

    @Test
    void 已待交付或已取消的訂單_不會被自動拒絕() {
        book("a1", SELLER_A, 200, null);
        String cancelled = orderService.createOrder(OTHER_BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();
        orderService.cancel(OTHER_BUYER, cancelled);
        String first = orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();

        orderService.accept(SELLER_A, first);

        assertEquals(Order.STATUS_CANCELLED, orderDb.get(cancelled).getStatus());
        assertNull(orderDb.get(cancelled).getRejectReason());
    }

    @Test
    void 被自動拒絕的訂單_不能再接受拒絕或取消_書仍保留中() {
        book("a1", SELLER_A, 200, null);
        String first = orderService.createOrder(BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();
        String second = orderService.createOrder(OTHER_BUYER, form(Order.DELIVERY_FACE_TO_FACE, "a1")).getId();
        orderService.accept(SELLER_A, first);

        assertThrows(InvalidOrderException.class, () -> orderService.accept(SELLER_A, second));
        assertThrows(InvalidOrderException.class, () -> orderService.reject(SELLER_A, second));
        assertThrows(InvalidOrderException.class, () -> orderService.cancel(OTHER_BUYER, second));
        assertEquals(Order.STATUS_REJECTED, orderDb.get(second).getStatus());
        assertEquals(Book.STATUS_RESERVED, bookStatus("a1"));
    }

    // ===== US11 確認收書、取消訂單 =====

    @Test
    void 買家在待交付的訂單按確認收到_訂單已完成_書已售出() {
        String id = acceptedOrder();

        Order order = orderService.confirmReceived(BUYER, id);

        assertEquals(Order.STATUS_COMPLETED, order.getStatus());
        assertEquals(Book.STATUS_SOLD, bookStatus("a1"));
        verify(bookRepository, times(2)).saveAll(any()); // 接受一次、確認一次
    }

    @Test
    void 待賣家回覆的訂單不能確認收到_狀態不變() {
        String id = pendingOrder();

        assertThrows(InvalidOrderException.class, () -> orderService.confirmReceived(BUYER, id));

        assertEquals(Order.STATUS_WAITING_SELLER, orderDb.get(id).getStatus());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("a1"));
    }

    @Test
    void 賣家嘗試替買家按確認收到_拒絕操作_訂單狀態不變() {
        String id = acceptedOrder();

        assertThrows(ForbiddenOperationException.class, () -> orderService.confirmReceived(SELLER_A, id));

        assertEquals(Order.STATUS_TO_DELIVER, orderDb.get(id).getStatus());
        assertEquals(Book.STATUS_RESERVED, bookStatus("a1"));
    }

    @Test
    void 交付前買家取消訂單_訂單已取消_書回到上架中() {
        String id = acceptedOrder();

        Order order = orderService.cancel(BUYER, id);

        assertEquals(Order.STATUS_CANCELLED, order.getStatus());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("a1"));
        verify(bookRepository, times(2)).saveAll(any()); // 接受一次、取消還原一次
    }

    @Test
    void 賣家也可以在交付前取消訂單() {
        String id = pendingOrder();

        Order order = orderService.cancel(SELLER_A, id);

        assertEquals(Order.STATUS_CANCELLED, order.getStatus());
        assertEquals(Book.STATUS_ON_SALE, bookStatus("a1"));
    }

    @Test
    void 已完成的訂單不能取消() {
        String id = acceptedOrder();
        orderService.confirmReceived(BUYER, id);

        assertThrows(InvalidOrderException.class, () -> orderService.cancel(BUYER, id));
        assertEquals(Book.STATUS_SOLD, bookStatus("a1"));
    }

    @Test
    void 不相干的人不能取消或查看訂單() {
        String id = pendingOrder();

        assertThrows(ForbiddenOperationException.class, () -> orderService.cancel(OTHER_BUYER, id));
        assertThrows(ForbiddenOperationException.class, () -> orderService.getOrder(OTHER_BUYER, id));
        assertEquals(Order.STATUS_WAITING_SELLER, orderDb.get(id).getStatus());
    }
}
