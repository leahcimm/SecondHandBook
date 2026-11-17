package ntou.cs.secondhandbook.controller;

import jakarta.servlet.http.HttpServletRequest;
import ntou.cs.secondhandbook.model.Order;
import ntou.cs.secondhandbook.model.OrderForm;
import ntou.cs.secondhandbook.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 訂單 API。每個請求都要帶目前使用者（見 CurrentUser）。
// 錯誤回應統一由 GlobalExceptionHandler 轉成 {"message": "..."}：
//   400 資料錯誤或狀態不對、401 沒帶使用者、403 不是該訂單的買家／賣家、404 找不到
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CurrentUser currentUser;

    // US04：POST /api/orders，JSON {"bookIds": [...], "deliveryMethod": "面交" 或 "寄送"}
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderForm form, HttpServletRequest request) {
        Order order = orderService.createOrder(currentUser.name(request), form);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // GET /api/orders?role=buyer（我送出的，預設）或 role=seller（我收到的）
    @GetMapping
    public List<Order> getOrders(@RequestParam(value = "role", defaultValue = "buyer") String role,
                                 HttpServletRequest request) {
        String user = currentUser.name(request);
        return "seller".equals(role) ? orderService.findOrdersAsSeller(user) : orderService.findOrdersAsBuyer(user);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable String id, HttpServletRequest request) {
        return orderService.getOrder(currentUser.name(request), id);
    }

    // US05：賣家接受
    @PostMapping("/{id}/accept")
    public Order accept(@PathVariable String id, HttpServletRequest request) {
        return orderService.accept(currentUser.name(request), id);
    }

    // US05：賣家拒絕
    @PostMapping("/{id}/reject")
    public Order reject(@PathVariable String id, HttpServletRequest request) {
        return orderService.reject(currentUser.name(request), id);
    }

    // US11：買家確認收到
    @PostMapping("/{id}/confirm")
    public Order confirm(@PathVariable String id, HttpServletRequest request) {
        return orderService.confirmReceived(currentUser.name(request), id);
    }

    // US11 / FR-OD-05：交付前買家或賣家取消
    @PostMapping("/{id}/cancel")
    public Order cancel(@PathVariable String id, HttpServletRequest request) {
        return orderService.cancel(currentUser.name(request), id);
    }
}
