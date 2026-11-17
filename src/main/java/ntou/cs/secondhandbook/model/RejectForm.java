package ntou.cs.secondhandbook.model;

// 管理員退回書籍時送來的 JSON：{"reason": "書況描述不足"}（US07）
public record RejectForm(String reason) {
}
