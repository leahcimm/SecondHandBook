package ntou.cs.secondhandbook;

import ntou.cs.secondhandbook.controller.CurrentUser;
import ntou.cs.secondhandbook.controller.OrderController;
import ntou.cs.secondhandbook.exception.ForbiddenOperationException;
import ntou.cs.secondhandbook.exception.InvalidOrderException;
import ntou.cs.secondhandbook.exception.OrderNotFoundException;
import ntou.cs.secondhandbook.model.Order;
import ntou.cs.secondhandbook.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 測試訂單 API 的回應碼與訊息，以及「目前使用者」標頭的解讀
@WebMvcTest(OrderController.class)
@Import(CurrentUser.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    // 前端會用 encodeURIComponent 編碼中文名稱
    private static String user(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8);
    }

    @Test
    void 送出訂單回201_買家名稱和表單都傳給Service() throws Exception {
        Order saved = new Order();
        saved.setStatus(Order.STATUS_WAITING_SELLER);
        saved.setTotalAmount(260);
        when(orderService.createOrder(eq("小美"), any())).thenReturn(saved);

        mockMvc.perform(post("/api/orders").header(CurrentUser.HEADER, user("小美"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookIds\":[\"a1\"],\"deliveryMethod\":\"寄送\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("待賣家回覆"))
                .andExpect(jsonPath("$.totalAmount").value(260));

        verify(orderService).createOrder(eq("小美"), argThat(f ->
                f.getBookIds().equals(List.of("a1")) && "寄送".equals(f.getDeliveryMethod())));
    }

    @Test
    void 沒帶使用者_回401_不呼叫Service() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("請先輸入你的名稱"));
        verifyNoInteractions(orderService);
    }

    @Test
    void 空購物車_回400與訊息() throws Exception {
        when(orderService.createOrder(any(), any()))
                .thenThrow(new InvalidOrderException("購物車沒有書籍，請先加入書籍"));

        mockMvc.perform(post("/api/orders").header(CurrentUser.HEADER, user("小美"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("購物車沒有書籍，請先加入書籍"));
    }

    @Test
    void 非該訂單的賣家接受_回403() throws Exception {
        when(orderService.accept("阿華", "o1"))
                .thenThrow(new ForbiddenOperationException("只有這筆訂單的賣家可以接受訂單"));

        mockMvc.perform(post("/api/orders/o1/accept").header(CurrentUser.HEADER, user("阿華")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只有這筆訂單的賣家可以接受訂單"));
    }

    @Test
    void 找不到訂單_回404() throws Exception {
        when(orderService.confirmReceived("小美", "nope")).thenThrow(new OrderNotFoundException("x"));

        mockMvc.perform(post("/api/orders/nope/confirm").header(CurrentUser.HEADER, user("小美")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("找不到這筆訂單"));
    }

    @Test
    void 賣家查收到的訂單_用role等於seller() throws Exception {
        when(orderService.findOrdersAsSeller("阿明")).thenReturn(List.of(new Order(), new Order()));

        mockMvc.perform(get("/api/orders").param("role", "seller").header(CurrentUser.HEADER, user("阿明")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 拒絕確認取消三個動作都接到Service() throws Exception {
        Order o = new Order();
        when(orderService.reject("阿明", "o1")).thenReturn(o);
        when(orderService.confirmReceived("小美", "o1")).thenReturn(o);
        when(orderService.cancel("小美", "o1")).thenReturn(o);

        mockMvc.perform(post("/api/orders/o1/reject").header(CurrentUser.HEADER, user("阿明")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/o1/confirm").header(CurrentUser.HEADER, user("小美")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/o1/cancel").header(CurrentUser.HEADER, user("小美")))
                .andExpect(status().isOk());

        verify(orderService).reject("阿明", "o1");
        verify(orderService).confirmReceived("小美", "o1");
        verify(orderService).cancel("小美", "o1");
    }
}
