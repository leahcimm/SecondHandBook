// 各頁面共用的東西：科目清單、書況等級、交付方式、文字跳脫、導覽列、暫代身分、訂單 API、購物車

// 科目清單（刊登頁的下拉選單、列表頁的篩選都用這份）
const SUBJECTS = [
    '計算機概論', '程式設計', '資料結構', '演算法', '離散數學',
    '微積分', '線性代數', '機率與統計', '計算機組織', '作業系統',
    '資料庫系統', '計算機網路', '軟體工程', '通識', '其他'
];

function fillSubjectOptions(select) {
    SUBJECTS.forEach(subject => {
        const option = document.createElement('option');
        option.value = subject;
        option.textContent = subject;
        select.appendChild(option);
    });
}

// 書況五級（和後端 Book.CONDITION_GRADES 同順序），說明會顯示在選單與卡片上
const CONDITION_GRADES = [
    { grade: '全新', description: '未使用，無任何痕跡' },
    { grade: '近全新', description: '翻閱過，無畫記、無折痕' },
    { grade: '良好', description: '少量畫記或筆記（10 頁以內），無缺頁' },
    { grade: '普通', description: '大量畫記或筆記，封面有磨損，無缺頁' },
    { grade: '有缺損', description: '有缺頁、破損或水漬，需在描述中說明' }
];

function fillConditionOptions(select, withDescription) {
    CONDITION_GRADES.forEach(item => {
        const option = document.createElement('option');
        option.value = item.grade;
        option.textContent = withDescription ? `${item.grade}：${item.description}` : item.grade;
        select.appendChild(option);
    });
}

function conditionDescription(grade) {
    const item = CONDITION_GRADES.find(c => c.grade === grade);
    return item ? item.description : '';
}

// 交付方式的文字：校內面交一律可以，可寄送時再加上運費
function deliveryText(book) {
    if (!book.shippable) return '校內面交';
    return book.shippingFee === 0 ? '校內面交／可寄送（免運費）' : `校內面交／可寄送（運費 ${book.shippingFee} 元）`;
}

// 使用者輸入的文字放進 HTML 前先跳脫，避免 <script> 被執行（NFR-04）
function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// ===== 共用導覽列 =====
// 每一頁放 <nav id="navbar"></nav>，再呼叫 renderNavbar()。
// 導覽列右邊有「你的名稱」欄位：這是登入前的暫代身分（T03 決定），T05 做登入後拿掉這一塊。
function renderNavbar(onUserChange) {
    const nav = document.getElementById('navbar');
    nav.className = 'navbar navbar-expand navbar-dark bg-primary mb-4';
    nav.innerHTML = `
        <div class="container flex-wrap gap-2">
            <a class="navbar-brand" href="index.html"><i class="fas fa-book"></i> 海大二手書交易平台</a>
            <div class="d-flex flex-wrap align-items-center gap-2">
                <a class="btn btn-light btn-sm" href="index.html"><i class="fas fa-list"></i> 書籍列表</a>
                <a class="btn btn-light btn-sm" href="sell.html"><i class="fas fa-plus"></i> 刊登書籍</a>
                <a class="btn btn-light btn-sm" href="cart.html"><i class="fas fa-shopping-cart"></i> 購物車
                    <span class="badge bg-danger" id="cartCount"></span></a>
                <a class="btn btn-light btn-sm" href="orders.html"><i class="fas fa-receipt"></i> 我的訂單</a>
                <div class="input-group input-group-sm" style="width: 220px;">
                    <span class="input-group-text"><i class="fas fa-user"></i></span>
                    <input type="text" class="form-control" id="userName" placeholder="你的名稱（暫代登入）"
                           title="和刊登書籍時填的賣家稱呼一樣，才能處理自己收到的訂單">
                </div>
            </div>
        </div>`;
    const input = document.getElementById('userName');
    input.value = getUserName();
    input.addEventListener('change', () => {
        setUserName(input.value.trim());
        input.value = getUserName();
        if (onUserChange) onUserChange();
    });
    updateCartCount();
}

// localStorage 在無痕視窗或被封鎖時可能丟錯，所以都包 try/catch
function storageGet(key, fallback) {
    try {
        const raw = localStorage.getItem(key);
        return raw === null ? fallback : JSON.parse(raw);
    } catch (e) {
        return fallback;
    }
}

function storageSet(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
        console.warn('無法存到瀏覽器：', e);
    }
}

function getUserName() {
    return storageGet('userName', '');
}

function setUserName(name) {
    storageSet('userName', name);
}

// ===== 呼叫訂單 API =====
// 自動帶上目前使用者的標頭 X-User-Name（中文要先 encodeURIComponent，否則 fetch 會出錯）。
// 後端錯誤統一是 {"message": "..."}，這裡轉成 Error 丟出，畫面再顯示 error.message。
async function api(path, options = {}) {
    const name = getUserName();
    if (!name) {
        throw new Error('請先在右上角輸入你的名稱');
    }
    const headers = Object.assign({ 'X-User-Name': encodeURIComponent(name) }, options.headers || {});
    if (options.body) headers['Content-Type'] = 'application/json';
    let response;
    try {
        response = await fetch(path, Object.assign({}, options, { headers }));
    } catch (e) {
        throw new Error('無法連線到伺服器，請稍後再試');
    }
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || `操作失敗（HTTP ${response.status}）`);
    }
    return data;
}

// ===== 購物車（存在瀏覽器，PLAN 技術決定） =====
// 每一項是加入當下書的資料：{ id, title, author, price, sellerName, shippable, shippingFee }
function getCart() {
    return storageGet('cart', []);
}

function saveCart(cart) {
    storageSet('cart', cart);
    updateCartCount();
}

function updateCartCount() {
    const badge = document.getElementById('cartCount');
    if (badge) {
        const count = getCart().length;
        badge.textContent = count > 0 ? count : '';
    }
}

// 書籍狀態：只有這兩種可以加入購物車（T06 審核做完後改成只有「上架中」）
const ORDERABLE_BOOK_STATUSES = ['上架中', '待審核'];

function showAlert(containerId, type, message) {
    document.getElementById(containerId).innerHTML =
        `<div class="alert alert-${type} alert-dismissible fade show">${escapeHtml(message)}` +
        '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>';
}
