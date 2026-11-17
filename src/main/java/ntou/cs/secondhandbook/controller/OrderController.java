package ntou.cs.secondhandbook.controller;

import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.Order;
import ntou.cs.secondhandbook.model.OrderForm;
import ntou.cs.secondhandbook.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 訂單 API。都要登入（SecurityConfig），目前使用者從 CurrentUser 取得。
// 錯誤回應統一由 GlobalExceptionHandler 轉成 {"message": "..."}：
//   400 資料錯誤或狀態不對、401 沒登入、403 不是該訂單的買家／賣家、404 找不到
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CurrentUser currentUser;

    // US04：POST /api/orders，JSON {"bookIds": [...], "deliveryMethod": "面交" 或 "寄送"}
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderForm form) {
        Order order = orderService.createOrder(currentUser.get(), form);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // GET /api/orders?role=buyer（我送出的，預設）或 role=seller（我收到的）
    @GetMapping
    public List<Order> getOrders(@RequestParam(value = "role", defaultValue = "buyer") String role) {
        LoginUser user = currentUser.get();
        return "seller".equals(role) ? orderService.findOrdersAsSeller(user) : orderService.findOrdersAsBuyer(user);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable String id) {
        return orderService.getOrder(currentUser.get(), id);
    }

    // US05：賣家接受
    @PostMapping("/{id}/accept")
    public Order accept(@PathVariable String id) {
        return orderService.accept(currentUser.get(), id);
    }

    // US05：賣家拒絕
    @PostMapping("/{id}/reject")
    public Order reject(@PathVariable String id) {
        return orderService.reject(currentUser.get(), id);
    }

    // US11：買家確認收到
    @PostMapping("/{id}/confirm")
    public Order confirm(@PathVariable String id) {
        return orderService.confirmReceived(currentUser.get(), id);
    }

    // US11 / FR-OD-05：交付前買家或賣家取消
    @PostMapping("/{id}/cancel")
    public Order cancel(@PathVariable String id) {
        return orderService.cancel(currentUser.get(), id);
    }
}
