# 海大二手書交易平台

國立臺灣海洋大學 115 學年度軟體工程期末專題（第六組），題目 G「二手書交易平台」。

本專案以課程提供的 BookReview 範例（Spring Boot + MongoDB）為基礎架構，改寫為二手書刊登與瀏覽功能。

## 文件與看板

- 需求文件（HackMD）：https://hackmd.io/@JG5S7CRvTyC3YMMPTVlj2A/r1RLAnxqze
- 使用者故事地圖（Miro）：https://miro.com/app/board/uXjVEcYsoOo=/?share_link_id=915811621196
- 專案看板（Trello）：https://trello.com/b/hiVszH61
- 實作影片：https://youtu.be/OoV9Ew2TyBw

各 Iteration 繳交時的文件存檔放在 `docs/`：

- `docs/iteration1/需求文件.md`：Iteration 1 需求文件（HackMD 匯出）
- `docs/iteration2/需求文件.md`：版次 0.2，依 Iteration 1 Review 老師建議修訂（交易確認、運費、書況、同書多賣家）
- `docs/iteration1/截圖/`、`docs/iteration1/範例封面/`：畫面截圖與 Demo 用書封
- `docs/lab1/`：Lab1 使用者故事看板
- `docs/環境安裝.md`：環境安裝踩雷提示

## Iteration 1 完成的功能

| 故事 | 內容 | 頁面 / API |
|---|---|---|
| US02 刊登書籍 | 填寫書名、作者、科目、書況、售價並上傳照片；前後端都會檢查必填欄位、售價為正整數、照片格式與大小；刊登後狀態為「待審核」 | `sell.html`、`POST /api/books` |
| US03 尋找書籍 | 卡片列表（照片、書名、科目、售價、書況、賣家），可用書名關鍵字搜尋、依科目篩選；沒有書時顯示「目前沒有書籍」 | `index.html`、`GET /api/books?keyword=&subject=` |

## Iteration 1 Review 後的修改（版次 0.2）

| 老師建議 | 程式上的修改 |
|---|---|
| 書況沒有標準 | 刊登時書況等級必選（全新／近全新／良好／普通／有缺損），原本的書況描述改為選填補充；列表可依書況篩選，卡片顯示等級 |
| 運費如何處理 | 刊登時預設校內面交；可勾選「也可以寄送」並填運費（0 以上的整數），列表卡片顯示交付方式與運費 |
| 多位賣家賣同一本書 | 列表預設依價格由低到高排序，也可改成最新刊登或書況由新到舊 |
| 交易確認 | 訂單功能（US04 之後）還沒實作，先寫進需求文件的訂單狀態流程 |

## 技術

- 後端：Java 17 以上、Spring Boot 3.3、Spring Data MongoDB
- 前端：HTML、JavaScript、Bootstrap 5
- 資料庫：MongoDB（本機 `mongodb://localhost:27017`，資料庫名稱 `secondhandbook`）

## 在自己電腦上執行

1. 安裝 JDK 17 以上與 MongoDB Community Server，並確認 MongoDB 正在執行。
   - Mac（Homebrew）：`brew services start mongodb-community`
   - Windows：安裝時勾選「Install MongoDB as a Service」即會自動執行。
2. 下載專案：`git clone https://github.com/leahcimm/SecondHandBook.git`
3. 在專案資料夾執行：
   - Mac：`chmod +x mvnw`（第一次）然後 `./mvnw spring-boot:run`
   - Windows：`mvnw.cmd spring-boot:run`
4. 看到 `Started SecondHandBookApplication` 後，打開 http://localhost:8080

## 自動測試

```
./mvnw test
```

共 25 個測試：`BookServiceTest`（刊登規則、書況等級、運費、搜尋篩選與排序，不需要 MongoDB）、`BookControllerTest`（API 回應碼與錯誤訊息）、`SecondHandBookApplicationTests`（程式能啟動）。

## API

| 方法 | 路徑 | 說明 |
|---|---|---|
| GET | `/api/books` | 書籍列表；可加 `keyword`（書名包含）、`subject`（科目）、`conditionGrade`（書況等級）、`sort`（`price` 價格低到高，預設／`newest` 最新刊登／`condition` 書況由新到舊） |
| GET | `/api/books/{id}` | 單本書籍 |
| POST | `/api/books` | 刊登書籍（`multipart/form-data`：title、author、subject、conditionGrade、condition、price、shippable、shippingFee、sellerName、sellerContact、image）。成功回 201；資料錯誤回 400 與 `{"message": "錯誤原因"}` |

## 專案結構

```
src/main/java/ntou/cs/secondhandbook/
├── model/        Book：書籍欄位；BookForm：刊登表單送進來的資料
├── repository/   BookRepository：資料庫存取與搜尋查詢
├── service/      BookService：刊登規則檢查、列表篩選與排序；FileStorageService：照片儲存
├── controller/   BookController：API /api/books
├── exception/    錯誤處理（統一回傳中文訊息）
└── config/       WebConfig：讓 /uploads/ 可以讀到上傳的照片
src/main/resources/
├── static/       index.html（列表頁）、sell.html（刊登頁）、js/common.js
└── application.properties
```

## 分支規則

- `main`：可以跑的版本，只由組長合併。
- 每個人開自己的分支，例如 `feature/book-form`，完成後發 Pull Request。
- Commit 訊息寫清楚做了什麼，例如「新增刊登表單的售價檢查」。

## 組員

| 學號 | 姓名 |
|---|---|
| 01157009 | 黃宇廷（組長） |
| 01257129 | 吳紀潓 |
| 01357006 | 李宥勳 |
| 01357136 | 楊孟芸 |
| 01357138 | 廖子宏 |
