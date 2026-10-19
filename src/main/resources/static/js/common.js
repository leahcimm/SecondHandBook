// 兩個頁面共用的東西：科目清單、書況等級、交付方式、文字跳脫

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
