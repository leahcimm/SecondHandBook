package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.config.SecurityConfig;
import ntou.cs.secondhandbook.controller.CurrentUser;
import ntou.cs.secondhandbook.controller.OrderController;
import ntou.cs.secondhandbook.exception.ForbiddenOperationException;
import ntou.cs.secondhandbook.exception.InvalidOrderException;
import ntou.cs.secondhandbook.exception.OrderNotFoundException;
import ntou.cs.secondhandbook.model.LoginUser;
import ntou.cs.secondhandbook.model.Order;
import ntou.cs.secondhandbook.service.AccountDetails;
import ntou.cs.secondhandbook.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 測試訂單 API 的回應碼與訊息，以及登入者有沒有正確交給 Service（T05 起改用登入，不再讀 X-User-Name）
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class OrderControllerTest {

    private static final LoginUser BUYER = new LoginUser("u1", "小美", "u1@mail.ntou.edu.tw", "IG 小美");
    private static final LoginUser SELLER = new LoginUser("uA", "阿明", "ua@mail.ntou.edu.tw", "IG 阿明");
    private static final LoginUser OTHER_SELLER = new LoginUser("uB", "阿華", "ub@mail.ntou.edu.tw", "IG 阿華");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    // 模擬「這個人已經登入」（Session 裡有他的 AccountDetails）
    private static RequestPostProcessor loggedIn(LoginUser loginUser) {
        return user(new AccountDetails(loginUser, null));
    }

    @Test
    void 送出訂單回201_登入者和表單都傳給Service() throws Exception {
        Order saved = new Order();
        saved.setStatus(Order.STATUS_WAITING_SELLER);
        saved.setTotalAmount(260);
        when(orderService.createOrder(eq(BUYER), any())).thenReturn(saved);

        mockMvc.perform(post("/api/orders").with(loggedIn(BUYER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookIds\":[\"a1\"],\"deliveryMethod\":\"寄送\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("待賣家回覆"))
                .andExpect(jsonPath("$.totalAmount").value(260));

        verify(orderService).createOrder(eq(BUYER), argThat(f ->
                f.getBookIds().equals(List.of("a1")) && "寄送".equals(f.getDeliveryMethod())));
    }

    @Test
    void 沒登入_每個訂單API都回401請先登入_不呼叫Service() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("請先登入"));
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("請先登入"));
        mockMvc.perform(post("/api/orders/o1/accept"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }

    @Test
    void 只帶舊的XUserName標頭_不算登入_回401() throws Exception {
        mockMvc.perform(get("/api/orders").header("X-User-Name", "%E5%B0%8F%E7%BE%8E"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }

    @Test
    void 空購物車_回400與訊息() throws Exception {
        when(orderService.createOrder(any(), any()))
                .thenThrow(new InvalidOrderException("購物車沒有書籍，請先加入書籍"));

        mockMvc.perform(post("/api/orders").with(loggedIn(BUYER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("購物車沒有書籍，請先加入書籍"));
    }

    @Test
    void 非該訂單的賣家接受_回403() throws Exception {
        when(orderService.accept(OTHER_SELLER, "o1"))
                .thenThrow(new ForbiddenOperationException("只有這筆訂單的賣家可以接受訂單"));

        mockMvc.perform(post("/api/orders/o1/accept").with(loggedIn(OTHER_SELLER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只有這筆訂單的賣家可以接受訂單"));
    }

    @Test
    void 找不到訂單_回404() throws Exception {
        when(orderService.confirmReceived(BUYER, "nope")).thenThrow(new OrderNotFoundException("x"));

        mockMvc.perform(post("/api/orders/nope/confirm").with(loggedIn(BUYER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("找不到這筆訂單"));
    }

    @Test
    void 賣家查收到的訂單_用role等於seller() throws Exception {
        when(orderService.findOrdersAsSeller(SELLER)).thenReturn(List.of(new Order(), new Order()));

        mockMvc.perform(get("/api/orders").param("role", "seller").with(loggedIn(SELLER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 拒絕確認取消三個動作都接到Service() throws Exception {
        Order o = new Order();
        when(orderService.reject(SELLER, "o1")).thenReturn(o);
        when(orderService.confirmReceived(BUYER, "o1")).thenReturn(o);
        when(orderService.cancel(BUYER, "o1")).thenReturn(o);

        mockMvc.perform(post("/api/orders/o1/reject").with(loggedIn(SELLER)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/o1/confirm").with(loggedIn(BUYER)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/o1/cancel").with(loggedIn(BUYER)))
                .andExpect(status().isOk());

        verify(orderService).reject(SELLER, "o1");
        verify(orderService).confirmReceived(BUYER, "o1");
        verify(orderService).cancel(BUYER, "o1");
    }
}
