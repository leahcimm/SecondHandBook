package ntou.cs.secondhandbook.service;

import ntou.cs.secondhandbook.exception.ForbiddenOperationException;
import ntou.cs.secondhandbook.exception.InvalidOrderException;
import ntou.cs.secondhandbook.exception.OrderNotFoundException;
import ntou.cs.secondhandbook.exception.ResourceNotFoundException;
import ntou.cs.secondhandbook.model.Book;
import ntou.cs.secondhandbook.model.Order;
import ntou.cs.secondhandbook.model.OrderForm;
import ntou.cs.secondhandbook.model.OrderItem;
import ntou.cs.secondhandbook.repository.BookRepository;
import ntou.cs.secondhandbook.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// 訂單流程：US04 送出訂單、US05 處理訂單、US11 確認收書與取消（FR-OD-02～05）
// 訂單狀態和書籍狀態要一起變，見需求文件第 6 章。
// 注意：「待賣家回覆」時書籍狀態還沒變，只有賣家接受才把書改成「保留中」。
// 所以拒絕、取消待賣家回覆的訂單都不動書；只有取消「待交付」的訂單才把書還原。
@Service
public class OrderService {

    // 可以下單的書籍狀態。
    // 暫時也允許「待審核」，因為還沒有審核功能，目前所有書都是待審核。T06 做審核後改成只允許「上架中」。
    static final Set<String> ORDERABLE_BOOK_STATUSES = Set.of(Book.STATUS_ON_SALE, Book.STATUS_PENDING);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BookRepository bookRepository;

    // ===== US04 送出訂單 =====

    public Order createOrder(String buyerName, OrderForm form) {
        List<String> bookIds = form.getBookIds() == null ? List.of()
                : form.getBookIds().stream().filter(Objects::nonNull).distinct().toList();
        if (bookIds.isEmpty()) {
            throw new InvalidOrderException("購物車沒有書籍，請先加入書籍");
        }
        String delivery = form.getDeliveryMethod();
        if (!Order.DELIVERY_FACE_TO_FACE.equals(delivery) && !Order.DELIVERY_SHIPPING.equals(delivery)) {
            throw new InvalidOrderException("請選擇交付方式（面交或寄送）");
        }

        List<Book> books = bookIds.stream().map(this::findBook).toList();

        Set<String> sellers = new LinkedHashSet<>();
        for (Book book : books) {
            if (!ORDERABLE_BOOK_STATUSES.contains(book.getStatus())) {
                throw new InvalidOrderException("《" + book.getTitle() + "》已被保留或售出，不能下單");
            }
            if (isBlank(book.getSellerName())) {
                throw new InvalidOrderException("《" + book.getTitle() + "》沒有賣家資料，不能下單");
            }
            sellers.add(book.getSellerName());
        }
        if (sellers.size() > 1) {
            throw new InvalidOrderException("一筆訂單只能包含同一位賣家的書，請分開送出");
        }
        String sellerName = sellers.iterator().next();
        if (sellerName.equals(buyerName)) {
            throw new InvalidOrderException("不能購買自己刊登的書");
        }

        // 寄送：每本都要可寄送；同一位賣家的書裝成一包，運費只收一次，取最高的那本（2026-11-17 Michael 決定）
        int shippingFee = 0;
        if (Order.DELIVERY_SHIPPING.equals(delivery)) {
            for (Book book : books) {
                if (!book.isShippable() || book.getShippingFee() == null) {
                    throw new InvalidOrderException("《" + book.getTitle() + "》不提供寄送，請改選面交");
                }
                shippingFee = Math.max(shippingFee, book.getShippingFee());
            }
        }
        int bookTotal = books.stream().mapToInt(Book::getPrice).sum();

        Order order = new Order();
        order.setBuyerName(buyerName);
        order.setSellerName(sellerName);
        order.setItems(new ArrayList<>(books.stream().map(OrderItem::new).toList()));
        order.setDeliveryMethod(delivery);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(bookTotal + shippingFee);
        order.setStatus(Order.STATUS_WAITING_SELLER);
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return orderRepository.save(order);
    }

    // ===== 查詢 =====

    public List<Order> findOrdersAsBuyer(String userName) {
        return orderRepository.findByBuyerNameOrderByCreatedAtDesc(userName);
    }

    public List<Order> findOrdersAsSeller(String userName) {
        return orderRepository.findBySellerNameOrderByCreatedAtDesc(userName);
    }

    // 只有這筆訂單的買家或賣家看得到
    public Order getOrder(String userName, String orderId) {
        Order order = findOrder(orderId);
        if (!userName.equals(order.getBuyerName()) && !userName.equals(order.getSellerName())) {
            throw new ForbiddenOperationException("你不是這筆訂單的買家或賣家");
        }
        return order;
    }

    // ===== US05 處理訂單（賣家） =====

    // 接受：訂單 → 待交付，書 → 保留中。接受前再確認一次書還能賣（可能已經接受了別人的訂單）
    public Order accept(String userName, String orderId) {
        Order order = findOrder(orderId);
        requireSeller(order, userName, "只有這筆訂單的賣家可以接受訂單");
        requireStatus(order, Order.STATUS_WAITING_SELLER, "只有「待賣家回覆」的訂單可以接受");

        List<Book> books = order.getItems().stream().map(item -> findBook(item.getBookId())).toList();
        for (Book book : books) {
            if (!ORDERABLE_BOOK_STATUSES.contains(book.getStatus())) {
                throw new InvalidOrderException("《" + book.getTitle() + "》已被保留或售出，無法接受這筆訂單");
            }
        }
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            order.getItems().get(i).setBookStatusBefore(book.getStatus());
            book.setStatus(Book.STATUS_RESERVED);
        }
        bookRepository.saveAll(books);
        return changeStatus(order, Order.STATUS_TO_DELIVER);
    }

    // 拒絕：訂單 → 已拒絕。書在待賣家回覆時沒被保留，所以不用動
    public Order reject(String userName, String orderId) {
        Order order = findOrder(orderId);
        requireSeller(order, userName, "只有這筆訂單的賣家可以拒絕訂單");
        requireStatus(order, Order.STATUS_WAITING_SELLER, "只有「待賣家回覆」的訂單可以拒絕");
        return changeStatus(order, Order.STATUS_REJECTED);
    }

    // ===== US11 確認收書、取消訂單 =====

    // 確認收到（只有買家）：訂單 → 已完成，書 → 已售出
    public Order confirmReceived(String userName, String orderId) {
        Order order = findOrder(orderId);
        if (!userName.equals(order.getBuyerName())) {
            throw new ForbiddenOperationException("只有這筆訂單的買家可以確認收到");
        }
        requireStatus(order, Order.STATUS_TO_DELIVER, "只有「待交付」的訂單可以確認收到");
        updateBooks(order, item -> Book.STATUS_SOLD);
        return changeStatus(order, Order.STATUS_COMPLETED);
    }

    // 取消（買家或賣家，交付前）：訂單 → 已取消。
    // 待交付的訂單書已被保留，要還原成接受前的狀態（原本待審核就回待審核，不會跳過審核）
    public Order cancel(String userName, String orderId) {
        Order order = findOrder(orderId);
        if (!userName.equals(order.getBuyerName()) && !userName.equals(order.getSellerName())) {
            throw new ForbiddenOperationException("只有這筆訂單的買家或賣家可以取消訂單");
        }
        String status = order.getStatus();
        if (Order.STATUS_TO_DELIVER.equals(status)) {
            updateBooks(order, item -> item.getBookStatusBefore() != null
                    ? item.getBookStatusBefore() : Book.STATUS_ON_SALE);
        } else if (!Order.STATUS_WAITING_SELLER.equals(status)) {
            throw new InvalidOrderException("只有「待賣家回覆」或「待交付」的訂單可以取消");
        }
        return changeStatus(order, Order.STATUS_CANCELLED);
    }

    // ===== 共用 =====

    private Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id " + orderId));
    }

    private Book findBook(String bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + bookId));
    }

    private static void requireSeller(Order order, String userName, String message) {
        if (!userName.equals(order.getSellerName())) {
            throw new ForbiddenOperationException(message);
        }
    }

    private static void requireStatus(Order order, String expected, String message) {
        if (!expected.equals(order.getStatus())) {
            throw new InvalidOrderException(message);
        }
    }

    // 把訂單裡每本書改成 newStatus 算出來的狀態；書已經被刪掉就跳過
    private void updateBooks(Order order, java.util.function.Function<OrderItem, String> newStatus) {
        List<Book> changed = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            bookRepository.findById(item.getBookId()).ifPresent(book -> {
                book.setStatus(newStatus.apply(item));
                changed.add(book);
            });
        }
        bookRepository.saveAll(changed);
    }

    private Order changeStatus(Order order, String status) {
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
