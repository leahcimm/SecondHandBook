// 兩個頁面共用的東西：科目清單、文字跳脫

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
