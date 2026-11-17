// 各頁面共用的東西：科目清單、書況等級、交付方式、文字跳脫、導覽列和登入者、呼叫 API、購物車

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

// ===== 共用導覽列和登入者 =====
// 每一頁放 <nav id="navbar"></nav>，再 await renderNavbar()，拿到登入者（沒登入是 null）。
// 登入者的資料是 {id, name, email, contact, admin}，從後端 GET /api/auth/me 取得（T05）。
// admin 是 true 時導覽列多一個「審核書籍」（T06）；只是決定要不要顯示，真正的權限由後端檢查。
let currentUser = null;

async function renderNavbar() {
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
                <a class="btn btn-light btn-sm" href="my-books.html"><i class="fas fa-book-open"></i> 我的刊登</a>
                <span class="d-flex align-items-center gap-2" id="userArea"></span>
            </div>
        </div>`;
    updateCartCount();
    currentUser = await fetchCurrentUser();
    const userArea = document.getElementById('userArea');
    if (currentUser) {
        userArea.innerHTML = `
            ${currentUser.admin ? '<a class="btn btn-warning btn-sm" href="admin.html" id="adminLink"><i class="fas fa-user-shield"></i> 審核書籍</a>' : ''}
            <span class="text-white" id="loginUserName"><i class="fas fa-user"></i> ${escapeHtml(currentUser.name)}</span>
            <button class="btn btn-outline-light btn-sm" id="logoutButton"><i class="fas fa-sign-out-alt"></i> 登出</button>`;
        document.getElementById('logoutButton').addEventListener('click', logout);
    } else {
        userArea.innerHTML = `
            <a class="btn btn-warning btn-sm" href="${loginUrl()}" id="loginLink"><i class="fas fa-sign-in-alt"></i> 登入</a>
            <a class="btn btn-outline-light btn-sm" href="register.html" id="registerLink">註冊</a>`;
    }
    return currentUser;
}

// 問後端目前是誰登入；沒登入（401）或連不上都當成沒登入
async function fetchCurrentUser() {
    try {
        const response = await fetch('/api/auth/me');
        return response.ok ? await response.json() : null;
    } catch (e) {
        return null;
    }
}

async function logout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
    } catch (e) {
        // 連不上也照樣回首頁
    }
    window.location.href = 'index.html';
}

// 登入頁的網址，帶上目前這一頁，登入後回到這裡
function loginUrl() {
    const here = window.location.pathname + window.location.search;
    return 'login.html?next=' + encodeURIComponent(here);
}

function goLogin() {
    window.location.href = loginUrl();
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

// ===== 呼叫要登入的 API =====
// 登入靠 Session Cookie，瀏覽器會自動帶上（同源），不用另外加標頭。
// 後端錯誤統一是 {"message": "..."}，這裡轉成 Error 丟出，畫面再顯示 error.message。
// 收到 401（沒登入或登入過期）時，直接帶使用者到登入頁，登入後回到原本那一頁。
async function api(path, options = {}) {
    const headers = Object.assign({}, options.headers || {});
    // body 是字串時才是 JSON；FormData（上傳照片）要讓瀏覽器自己設 Content-Type
    if (typeof options.body === 'string') headers['Content-Type'] = 'application/json';
    let response;
    try {
        response = await fetch(path, Object.assign({}, options, { headers }));
    } catch (e) {
        throw new Error('無法連線到伺服器，請稍後再試');
    }
    if (response.status === 401) {
        goLogin();
        throw new Error('請先登入');
    }
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || `操作失敗（HTTP ${response.status}）`);
    }
    return data;
}

// ===== 購物車（存在瀏覽器，PLAN 技術決定） =====
// 每一項是加入當下書的資料：{ id, title, author, price, sellerId, sellerName, shippable, shippingFee }
// 賣家用 sellerId 分辨（T12），姓名可能重複。T04 存的舊項目沒有 sellerId，讀出來時直接略過。
function getCart() {
    const cart = storageGet('cart', []);
    return Array.isArray(cart) ? cart.filter(item => item && item.sellerId) : [];
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

// 書籍狀態：只有管理員核准過的「上架中」可以加入購物車（T06，和後端 OrderService 一致）
const ORDERABLE_BOOK_STATUSES = ['上架中'];

// 書籍狀態的標籤顏色（我的刊登、審核頁用）
const BOOK_STATUS_BADGE = {
    '待審核': 'bg-warning text-dark',
    '上架中': 'bg-success',
    '已退回': 'bg-danger',
    '保留中': 'bg-info text-dark',
    '已售出': 'bg-secondary'
};

function formatTime(time) {
    return time ? time.replace('T', ' ').substring(0, 16) : '';
}

function showAlert(containerId, type, message) {
    document.getElementById(containerId).innerHTML =
        `<div class="alert alert-${type} alert-dismissible fade show">${escapeHtml(message)}` +
        '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>';
}
