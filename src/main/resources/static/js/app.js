// 全局变量
let currentTaskId = null;
let statusCheckInterval = null;

// 页面导航函数
function navigateToTextGeneration() {
    // 隐藏功能选择卡片
    document.getElementById('feature-cards').style.display = 'none';
    // 显示文本生成页面
    document.getElementById('text-generation-page').style.display = 'block';
    // 隐藏图片生成页面
    document.getElementById('image-generation-page').style.display = 'none';
}

function navigateToImageGeneration() {
    // 隐藏功能选择卡片
    document.getElementById('feature-cards').style.display = 'none';
    // 隐藏文本生成页面
    document.getElementById('text-generation-page').style.display = 'none';
    // 显示图片生成页面
    document.getElementById('image-generation-page').style.display = 'block';
}

function backToHome() {
    // 显示功能选择卡片
    document.getElementById('feature-cards').style.display = 'block';
    // 隐藏文本生成页面
    document.getElementById('text-generation-page').style.display = 'none';
    // 隐藏图片生成页面
    document.getElementById('image-generation-page').style.display = 'none';
    // 隐藏状态面板
    if (elements.statusPanel) {
        hideStatusPanel();
    }
}

// 将函数暴露到全局作用域
window.navigateToTextGeneration = navigateToTextGeneration;
window.navigateToImageGeneration = navigateToImageGeneration;
window.backToHome = backToHome;

// DOM 元素
const elements = {
    // 导航和标签页
    navLinks: document.querySelectorAll('.nav-link'),
    tabButtons: document.querySelectorAll('.tab-btn'),
    tabContents: document.querySelectorAll('.tab-content'),
    
    // 表单
    textForm: document.getElementById('text-form'),
    imageForm: document.getElementById('image-form'),
    
    // 输入元素
    textInput: document.getElementById('text-input'),
    textCounter: document.getElementById('text-counter'),
    imageInput: document.getElementById('image-input'),
    imageDescription: document.getElementById('image-description'),
    descCounter: document.getElementById('desc-counter'),
    
    // 文件上传
    fileUploadArea: document.getElementById('file-upload-area'),
    imagePreview: document.getElementById('image-preview'),
    previewImg: document.getElementById('preview-img'),
    removeImage: document.getElementById('remove-image'),
    
    // 状态面板
    statusPanel: document.getElementById('status-panel'),
    closeStatus: document.getElementById('close-status'),
    currentTaskId: document.getElementById('current-task-id'),
    currentStatus: document.getElementById('current-status'),
    progressFill: document.getElementById('progress-fill'),
    progressPercent: document.getElementById('progress-percent'),
    estimatedTime: document.getElementById('estimated-time'),
    statusMessage: document.getElementById('status-message'),
    resultPanel: document.getElementById('result-panel'),
    modelInfo: document.getElementById('model-info'),
    
    // 预览相关
    previewBtn: document.getElementById('preview-btn'),
    downloadBtn: document.getElementById('download-btn'),
    
    // 预览模态框
    previewModal: document.getElementById('preview-modal'),
    closePreview: document.getElementById('close-preview'),
    modelPreviewImg: document.getElementById('model-preview-img'),
    
    // 加载覆盖层
    loadingOverlay: document.getElementById('loading-overlay'),
    
    // 历史记录页面
    historySection: document.getElementById('history-section'),
    historyFilters: document.querySelectorAll('.history-filter'),
    refreshHistoryBtn: document.getElementById('refresh-history'),
    historyLoading: document.getElementById('history-loading'),
    historyEmpty: document.getElementById('history-empty'),
    historyList: document.getElementById('history-list'),
    historyPagination: document.getElementById('history-pagination')
};

// 初始化应用
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

function initializeApp() {
    setupNavigation();
    setupTabSwitching();
    setupFormHandlers();
    setupFileUpload();
    setupCharacterCounters();
    setupEventListeners();
    setupHistoryPage();
    initHealthStatusModal();
    
    console.log('3D模型生成器已初始化');
}

// 顶部导航处理
function setupNavigation() {
    elements.navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const href = link.getAttribute('href');
            const page = href.substring(1); // 移除 # 符号
            
            // 更新导航链接状态
            elements.navLinks.forEach(navLink => {
                navLink.classList.remove('active');
            });
            link.classList.add('active');
            
            // 处理页面切换
            handlePageSwitch(page);
        });
    });
}

function handlePageSwitch(page) {
    // 隐藏所有主要内容区域
    const heroSection = document.querySelector('.hero');
    const tabsSection = document.querySelector('.generation-tabs');
    const tabContents = document.querySelectorAll('.tab-content');
    const adminSection = document.getElementById('admin-section');
    
    if (page === 'home') {
        // 显示首页内容
        heroSection.style.display = 'block';
        tabsSection.style.display = 'block';
        tabContents.forEach(content => content.style.display = 'block');
        elements.historySection.style.display = 'none';
        if (adminSection) adminSection.style.display = 'none';
    } else if (page === 'history') {
        // 显示历史记录页面
        heroSection.style.display = 'none';
        tabsSection.style.display = 'none';
        tabContents.forEach(content => content.style.display = 'none');
        elements.historySection.style.display = 'block';
        if (adminSection) adminSection.style.display = 'none';
        loadHistoryData();
    } else if (page === 'admin') {
        // 显示管理员页面
        heroSection.style.display = 'none';
        tabsSection.style.display = 'none';
        tabContents.forEach(content => content.style.display = 'none');
        elements.historySection.style.display = 'none';
        if (adminSection) {
            adminSection.style.display = 'block';
            loadAdminDashboard();
        }
    }
}

// 选项卡切换
function setupTabSwitching() {
    elements.tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const tabId = button.dataset.tab;
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    // 更新按钮状态
    elements.tabButtons.forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tabId);
    });
    
    // 更新内容显示
    elements.tabContents.forEach(content => {
        content.classList.toggle('active', content.id === `${tabId}-tab`);
    });
    
    // 如果切换到历史记录页面，显示历史记录区域并加载数据
    if (tabId === 'history') {
        elements.historySection.style.display = 'block';
        loadHistoryData();
    } else {
        elements.historySection.style.display = 'none';
    }
}

// 表单处理
function setupFormHandlers() {
    // 文本生成表单
    elements.textForm.addEventListener('submit', handleTextGeneration);
    
    // 图片生成表单
    elements.imageForm.addEventListener('submit', handleImageGeneration);
}

async function handleTextGeneration(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const prompt = formData.get('text');
    const complexity = formData.get('complexity');
    const format = formData.get('format');
    
    // 验证输入
    if (!prompt.trim()) {
        showError('请输入模型描述');
        return;
    }
    
    if (prompt.length > 1024) {
        showError('文本描述不能超过1024个字符');
        return;
    }
    
    try {
        showLoading(true);
        
        // 构建URL参数
        const params = new URLSearchParams();
        params.append('prompt', prompt);
        if (format && format !== 'OBJ') {
            params.append('resultFormat', format);
        }
        // 可以根据需要添加enablePBR参数
        // params.append('enablePBR', false);
        
        const response = await fetch('/api/v1/ai3d/generate/text', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: params
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            currentTaskId = result.data.jobId;  // 注意：腾讯云AI3D返回的是jobId，不是taskId
            showStatusPanel({
                taskId: result.data.jobId,
                status: 'PROCESSING',
                requestId: result.data.requestId
            });
            startStatusPolling();
        } else {
            showError(result.message || '生成请求失败');
        }
    } catch (error) {
        console.error('文本生成请求失败:', error);
        showError('网络错误，请稍后重试');
    } finally {
        showLoading(false);
    }
}

async function handleImageGeneration(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    
    // 验证图片
    const imageFile = formData.get('image');
    if (!imageFile || imageFile.size === 0) {
        showError('请选择要上传的图片');
        return;
    }
    
    if (imageFile.size > 10 * 1024 * 1024) { // 10MB
        showError('图片文件大小不能超过10MB');
        return;
    }
    
    try {
        showLoading(true);
        
        // 将图片转换为Base64
        const imageBase64 = await fileToBase64(imageFile);
        
        // 构建请求参数
        const requestFormData = new FormData();
        requestFormData.append('imageBase64', imageBase64);
        
        // 添加可选参数
        const format = formData.get('format');
        const enablePBR = formData.get('enablePBR');
        if (format) {
            requestFormData.append('format', format);
        }
        if (enablePBR) {
            requestFormData.append('enablePBR', enablePBR);
        }
        
        const response = await fetch('/api/v1/ai3d/submit/image-base64', {
            method: 'POST',
            body: requestFormData
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            currentTaskId = result.data.jobId;
            showStatusPanel(result.data);
            startStatusPolling();
        } else {
            showError(result.message || '生成请求失败');
        }
    } catch (error) {
        console.error('图片生成请求失败:', error);
        showError('网络错误，请稍后重试');
    } finally {
        showLoading(false);
    }
}

// 将文件转换为Base64的辅助函数
function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            // 移除data:image/...;base64,前缀，只保留Base64数据
            const base64 = reader.result.split(',')[1];
            resolve(base64);
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

// 文件上传处理
function setupFileUpload() {
    const fileInput = elements.imageInput;
    const uploadArea = elements.fileUploadArea;
    
    // 点击上传区域触发文件选择
    uploadArea.addEventListener('click', (event) => {
        // 防止事件冒泡和重复触发
        event.preventDefault();
        event.stopPropagation();
        
        // 只有在没有预览图片时才触发文件选择
        if (!elements.imagePreview.style.display || elements.imagePreview.style.display === 'none') {
            fileInput.click();
        }
    });
    
    // 文件选择变化事件
    fileInput.addEventListener('change', handleFileSelect);
    
    // 拖拽上传事件
    uploadArea.addEventListener('dragover', handleDragOver);
    uploadArea.addEventListener('dragleave', handleDragLeave);
    uploadArea.addEventListener('drop', handleFileDrop);
    
    // 移除图片按钮事件
    elements.removeImage.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        removeSelectedImage();
    });
}

function handleFileSelect(event) {
    const file = event.target.files[0];
    if (file) {
        displayImagePreview(file);
    }
}

function handleDragOver(event) {
    event.preventDefault();
    elements.fileUploadArea.classList.add('dragover');
}

function handleDragLeave(event) {
    event.preventDefault();
    elements.fileUploadArea.classList.remove('dragover');
}

function handleFileDrop(event) {
    event.preventDefault();
    elements.fileUploadArea.classList.remove('dragover');
    
    const files = event.dataTransfer.files;
    if (files.length > 0) {
        const file = files[0];
        if (file.type.startsWith('image/')) {
            elements.imageInput.files = files;
            displayImagePreview(file);
        } else {
            showError('请选择图片文件');
        }
    }
}

function displayImagePreview(file) {
    const reader = new FileReader();
    reader.onload = function(e) {
        elements.previewImg.src = e.target.result;
        elements.imagePreview.style.display = 'block';
        elements.fileUploadArea.querySelector('.upload-placeholder').style.display = 'none';
    };
    reader.readAsDataURL(file);
}

function removeSelectedImage() {
    elements.imageInput.value = '';
    elements.imagePreview.style.display = 'none';
    elements.fileUploadArea.querySelector('.upload-placeholder').style.display = 'block';
}

// 字符计数器
function setupCharacterCounters() {
    elements.textInput.addEventListener('input', () => {
        updateCharacterCounter(elements.textInput, elements.textCounter, 1000);
    });
    
    elements.imageDescription.addEventListener('input', () => {
        updateCharacterCounter(elements.imageDescription, elements.descCounter, 500);
    });
}

function updateCharacterCounter(input, counter, maxLength) {
    const currentLength = input.value.length;
    counter.textContent = currentLength;
    
    if (currentLength > maxLength * 0.9) {
        counter.style.color = '#ff7675';
    } else if (currentLength > maxLength * 0.7) {
        counter.style.color = '#fdcb6e';
    } else {
        counter.style.color = '#666';
    }
}

// 事件监听器
function setupEventListeners() {
    // 关闭状态面板
    elements.closeStatus.addEventListener('click', hideStatusPanel);
    
    // 预览和下载按钮
    elements.previewBtn.addEventListener('click', showModelPreview);
    elements.downloadBtn.addEventListener('click', downloadModel);
    
    // 关闭预览模态框
    elements.closePreview.addEventListener('click', hidePreviewModal);
    elements.previewModal.addEventListener('click', (e) => {
        if (e.target === elements.previewModal) {
            hidePreviewModal();
        }
    });
    
    // ESC 键关闭模态框
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            if (elements.previewModal.style.display !== 'none') {
                hidePreviewModal();
            } else if (elements.statusPanel.style.display !== 'none') {
                hideStatusPanel();
            }
        }
    });
}

// 状态面板管理
function showStatusPanel(taskData) {
    elements.currentTaskId.textContent = taskData.jobId || taskData.taskId;
    elements.currentStatus.textContent = taskData.status || 'SUBMITTED';
    elements.currentStatus.className = `status-badge ${(taskData.status || 'submitted').toLowerCase()}`;
    
    if (taskData.estimatedTime) {
        elements.estimatedTime.textContent = `预计时间: ${taskData.estimatedTime}秒`;
    }
    
    elements.progressFill.style.width = '0%';
    elements.progressPercent.textContent = '0%';
    elements.statusMessage.textContent = '正在初始化...';
    elements.resultPanel.style.display = 'none';
    
    elements.statusPanel.style.display = 'block';
}

function hideStatusPanel() {
    elements.statusPanel.style.display = 'none';
    stopStatusPolling();
    currentTaskId = null;
}

function updateStatusPanel(statusData) {
    // 腾讯AI3D状态映射：WAIT -> PENDING, RUN -> PROCESSING, DONE -> COMPLETED, FAIL -> FAILED
    const statusMapping = {
        'WAIT': 'PENDING',
        'RUN': 'PROCESSING', 
        'DONE': 'COMPLETED',
        'FAIL': 'FAILED'
    };
    
    const mappedStatus = statusMapping[statusData.status] || statusData.status;
    
    elements.currentStatus.textContent = mappedStatus;
    elements.currentStatus.className = `status-badge ${mappedStatus.toLowerCase()}`;
    
    // 根据状态计算进度
    let progress = 0;
    switch (statusData.status) {
        case 'WAIT':
            progress = 10;
            break;
        case 'RUN':
            progress = 50; // 运行中显示50%
            break;
        case 'DONE':
            progress = 100;
            break;
        case 'FAIL':
            progress = 0;
            break;
        default:
            progress = statusData.progress || 0;
    }
    
    elements.progressFill.style.width = `${progress}%`;
    elements.progressPercent.textContent = `${progress}%`;
    
    // 更新状态消息
    const statusMessages = {
        'WAIT': '任务已创建，等待处理...',
        'RUN': '正在生成3D模型...',
        'DONE': '模型生成完成！',
        'FAIL': '生成失败，请重试'
    };
    
    elements.statusMessage.textContent = statusMessages[statusData.status] || statusData.errorMessage || '处理中...';
    
    // 如果完成，显示结果面板
    if (statusData.status === 'DONE') {
        showResultPanel(statusData);
        stopStatusPolling();
    } else if (statusData.status === 'FAIL') {
        stopStatusPolling();
    }
}

function showResultPanel(statusData) {
    elements.resultPanel.style.display = 'block';
    
    // 更新模型信息 - 适配腾讯云AI3D数据结构
    if (statusData.resultFile3Ds && statusData.resultFile3Ds.length > 0) {
        const file3D = statusData.resultFile3Ds[0];
        elements.modelInfo.innerHTML = `
            <div><strong>模型信息:</strong></div>
            <div>文件格式: ${file3D.format || 'N/A'}</div>
            <div>文件大小: ${file3D.fileSize ? formatFileSize(file3D.fileSize) : 'N/A'}</div>
            <div>下载地址: ${file3D.modelUrl ? '可用' : '不可用'}</div>
            <div>预览图: ${file3D.previewImageUrl ? '可用' : '不可用'}</div>
        `;
    } else {
        elements.modelInfo.innerHTML = `
            <div><strong>模型信息:</strong></div>
            <div>暂无模型文件信息</div>
        `;
    }
}

// 状态轮询
function startStatusPolling() {
    if (statusCheckInterval) {
        clearInterval(statusCheckInterval);
    }
    
    statusCheckInterval = setInterval(async () => {
        if (currentTaskId) {
            await checkTaskStatus();
        }
    }, 2000); // 每2秒检查一次
}

function stopStatusPolling() {
    if (statusCheckInterval) {
        clearInterval(statusCheckInterval);
        statusCheckInterval = null;
    }
}

async function checkTaskStatus() {
    try {
        const response = await fetch(`/api/v1/ai3d/query/${currentTaskId}`);
        const result = await response.json();
        
        if (result.code === 200) {
            updateStatusPanel(result.data);
        } else {
            console.error('状态查询失败:', result.message);
        }
    } catch (error) {
        console.error('状态查询网络错误:', error);
    }
}

// 模型预览
async function showModelPreview() {
    if (!currentTaskId) return;
    
    try {
        showLoading(true);
        
        const previewUrl = `/api/v1/ai3d/preview/${currentTaskId}?size=medium`;
        elements.modelPreviewImg.src = previewUrl;
        elements.previewModal.style.display = 'flex';
        
    } catch (error) {
        console.error('预览加载失败:', error);
        showError('预览加载失败');
    } finally {
        showLoading(false);
    }
}

function hidePreviewModal() {
    elements.previewModal.style.display = 'none';
}

// changePreviewAngle函数已移除，因为不再需要角度切换

// 模型下载
function downloadModel() {
    if (!currentTaskId) return;
    
    const downloadUrl = `/api/v1/ai3d/download/${currentTaskId}`;
    
    // 创建隐藏的下载链接
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `model_${currentTaskId}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// 工具函数
function showLoading(show) {
    elements.loadingOverlay.style.display = show ? 'flex' : 'none';
}

function showError(message) {
    // 简单的错误提示，可以后续改为更美观的提示框
    alert('错误: ' + message);
}

function showSuccess(message) {
    // 简单的成功提示，可以后续改为更美观的提示框
    alert('成功: ' + message);
}

// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 格式化时间
function formatTime(seconds) {
    if (seconds < 60) {
        return `${seconds}秒`;
    } else if (seconds < 3600) {
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        return `${minutes}分${remainingSeconds}秒`;
    } else {
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        return `${hours}小时${minutes}分钟`;
    }
}

// 调试函数
function debugLog(message, data) {
    if (console && console.log) {
        console.log(`[3D Generator] ${message}`, data || '');
    }
}

// ==================== 历史记录页面功能 ====================

// 历史记录相关变量
let currentHistoryPage = 1;
let currentHistoryStatus = 'all';
const historyPageSize = 10;

// 设置历史记录页面
function setupHistoryPage() {
    // 状态筛选器事件
    elements.historyFilters.forEach(filter => {
        filter.addEventListener('click', () => {
            // 更新筛选器状态
            elements.historyFilters.forEach(f => f.classList.remove('active'));
            filter.classList.add('active');
            
            // 更新当前状态并重新加载数据
            currentHistoryStatus = filter.dataset.status;
            currentHistoryPage = 1;
            loadHistoryData();
        });
    });
    
    // 刷新按钮事件
    elements.refreshHistoryBtn.addEventListener('click', () => {
        currentHistoryPage = 1;
        loadHistoryData();
    });
}

// 加载历史记录数据
async function loadHistoryData() {
    try {
        showHistoryLoading(true);
        
        // 构建请求参数
        const params = new URLSearchParams({
            page: currentHistoryPage - 1, // 后端使用0开始的页码
            size: historyPageSize
        });
        
        if (currentHistoryStatus !== 'all') {
            params.append('status', currentHistoryStatus.toUpperCase());
        }
        
        // 发送请求
        const response = await fetch(`/api/v1/models/history?${params}`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        
        // 显示历史记录
        displayHistoryData(data);
        
    } catch (error) {
        console.error('加载历史记录失败:', error);
        showError('加载历史记录失败: ' + error.message);
        showHistoryEmpty(true);
    } finally {
        showHistoryLoading(false);
    }
}

// 显示历史记录数据
function displayHistoryData(data) {
    // 修复：使用后端实际返回的数据结构
    const { items, totalPages, page: currentPage, total: totalElements } = data;
    
    if (!items || items.length === 0) {
        showHistoryEmpty(true);
        elements.historyPagination.style.display = 'none';
        return;
    }
    
    showHistoryEmpty(false);
    
    // 清空现有列表
    elements.historyList.innerHTML = '';
    
    // 渲染历史记录项
    items.forEach(item => {
        const historyItem = createHistoryItem(item);
        elements.historyList.appendChild(historyItem);
    });
    
    // 更新分页 - 注意currentPage已经是1开始的页码
    updateHistoryPagination(totalPages, currentPage, totalElements);
}

// 创建历史记录项元素
function createHistoryItem(item) {
    const div = document.createElement('div');
    div.className = 'history-item';
    
    // 格式化时间
    const createdAt = new Date(item.createdAt).toLocaleString('zh-CN');
    const completedAt = item.completedAt ? new Date(item.completedAt).toLocaleString('zh-CN') : '-';
    
    // 状态显示文本
    const statusText = {
        'PENDING': '等待中',
        'PROCESSING': '处理中',
        'COMPLETED': '已完成',
        'FAILED': '失败'
    }[item.status] || item.status;
    
    div.innerHTML = `
        <div class="history-item-header">
            <div class="history-item-id">任务ID: ${item.taskId}</div>
            <div class="history-item-status">
                <span class="status-badge ${item.status.toLowerCase()}">${statusText}</span>
            </div>
        </div>
        <div class="history-item-content">
            <div class="history-item-prompt">
                <strong>提示词:</strong> ${item.prompt || '无'}
            </div>
            <div class="history-item-details">
                <div class="history-detail">
                    <span class="detail-label">创建时间:</span>
                    <span class="detail-value">${createdAt}</span>
                </div>
                <div class="history-detail">
                    <span class="detail-label">完成时间:</span>
                    <span class="detail-value">${completedAt}</span>
                </div>
                ${item.duration ? `
                <div class="history-detail">
                    <span class="detail-label">耗时:</span>
                    <span class="detail-value">${formatTime(item.duration)}</span>
                </div>
                ` : ''}
                ${item.errorMessage ? `
                <div class="history-detail error">
                    <span class="detail-label">错误信息:</span>
                    <span class="detail-value">${item.errorMessage}</span>
                </div>
                ` : ''}
            </div>
        </div>
        ${item.status === 'COMPLETED' && (item.modelUrl || item.previewUrl) ? `
        <div class="history-item-actions">
            ${item.previewUrl ? `
            <button class="btn btn-secondary" onclick="previewHistoryModel('${item.previewUrl}')">
                预览模型
            </button>
            ` : ''}
            ${item.modelUrl ? `
            <button class="btn btn-primary" onclick="downloadHistoryModel('${item.modelUrl}', '${item.taskId}')">
                下载模型
            </button>
            ` : ''}
        </div>
        ` : ''}
    `;
    
    return div;
}

// 更新历史记录分页
function updateHistoryPagination(totalPages, currentPage, totalElements) {
    if (totalPages <= 1) {
        elements.historyPagination.style.display = 'none';
        return;
    }
    
    elements.historyPagination.style.display = 'flex';
    elements.historyPagination.innerHTML = `
        <div class="pagination-info">
            共 ${totalElements} 条记录，第 ${currentPage} / ${totalPages} 页
        </div>
        <div class="pagination-controls">
            <button class="btn btn-secondary" ${currentPage <= 1 ? 'disabled' : ''} 
                    onclick="goToHistoryPage(${currentPage - 1})">
                上一页
            </button>
            <button class="btn btn-secondary" ${currentPage >= totalPages ? 'disabled' : ''} 
                    onclick="goToHistoryPage(${currentPage + 1})">
                下一页
            </button>
        </div>
    `;
}

// 跳转到指定页面
function goToHistoryPage(page) {
    if (page < 1) return;
    currentHistoryPage = page;
    loadHistoryData();
}

// 显示/隐藏历史记录加载状态
function showHistoryLoading(show) {
    elements.historyLoading.style.display = show ? 'block' : 'none';
    elements.historyList.style.display = show ? 'none' : 'block';
}

// 显示/隐藏历史记录空状态
function showHistoryEmpty(show) {
    elements.historyEmpty.style.display = show ? 'block' : 'none';
    elements.historyList.style.display = show ? 'none' : 'block';
}

// 预览历史记录中的模型
function previewHistoryModel(previewUrl) {
    elements.modelPreviewImg.src = previewUrl;
    elements.previewModal.style.display = 'flex';
}

// 下载历史记录中的模型
function downloadHistoryModel(modelUrl, taskId) {
    const link = document.createElement('a');
    link.href = modelUrl;
    link.download = `3d-model-${taskId}.zip`;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// ==================== 管理员仪表板功能 ====================

// 初始化管理员仪表板
function initializeAdminDashboard() {
    // 刷新按钮事件
    const refreshBtn = document.getElementById('refresh-dashboard');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            loadAdminDashboard();
        });
    }
    
    // 指标天数选择器事件
    const metricsSelect = document.getElementById('metrics-days');
    if (metricsSelect) {
        metricsSelect.addEventListener('change', function() {
            loadMetricsChart(this.value);
        });
    }
}

// 将函数暴露到全局作用域
window.initializeAdminDashboard = initializeAdminDashboard;
window.loadAdminDashboard = loadAdminDashboard;

// 显示管理员页面
function showAdminSection() {
    // 隐藏其他section
    hideAllSections();
    
    // 显示管理员section
    const adminSection = document.getElementById('admin-section');
    if (adminSection) {
        adminSection.style.display = 'block';
        loadAdminDashboard();
    }
}

// 加载管理员仪表板数据
async function loadAdminDashboard() {
    try {
        // 显示加载状态
        showAdminLoadingState();
        
        // 并行加载所有数据
        await Promise.all([
            loadOverviewData(),
            loadHealthStatus(),
            loadPopularPrompts(),
            loadMetricsChart(7)
        ]);
        
    } catch (error) {
        console.error('加载仪表板数据失败:', error);
        showAdminErrorState('加载数据失败，请稍后重试');
    }
}

// 加载概览数据
async function loadOverviewData() {
    try {
        const response = await fetch('/admin/dashboard/overview');
        if (!response.ok) {
            throw new Error('获取概览数据失败');
        }
        
        const data = await response.json();
        
        // 更新概览卡片
        updateElement('today-tasks', data.todayTasks || 0);
        updateElement('success-rate', (data.successRate || 0) + '%');
        updateElement('avg-time', (data.avgProcessingTime || 0) + 's');
        updateElement('avg-rating', (data.avgRating || 0).toFixed(1));
        
    } catch (error) {
        console.error('加载概览数据失败:', error);
        // 显示默认值
        updateElement('today-tasks', '暂无数据');
        updateElement('success-rate', '暂无数据');
        updateElement('avg-time', '暂无数据');
        updateElement('avg-rating', '暂无数据');
    }
}

// 加载系统健康状态
async function loadHealthStatus() {
    try {
        const response = await fetch('/admin/dashboard/health');
        if (!response.ok) {
            throw new Error('获取健康状态失败');
        }
        
        const result = await response.json();
        
        if (result.code === 200 && result.data) {
            // 更新健康状态
            updateHealthStatus('db-status', result.data.database);
            updateHealthStatus('ai-status', result.data.aiService);
            updateHealthStatus('storage-status', result.data.storage);
        } else {
            throw new Error(result.message || '获取健康状态失败');
        }
        
    } catch (error) {
        console.error('加载健康状态失败:', error);
        // 显示错误状态
        updateHealthStatus('db-status', { status: 'error', message: '检查失败' });
        updateHealthStatus('ai-status', { status: 'error', message: '检查失败' });
        updateHealthStatus('storage-status', { status: 'error', message: '检查失败' });
    }
}

// 加载热门提示词
async function loadPopularPrompts() {
    try {
        const response = await fetch('/admin/dashboard/metrics');
        if (!response.ok) {
            throw new Error('获取热门提示词失败');
        }
        
        const result = await response.json();
        const container = document.getElementById('popular-prompts');
        
        if (result.code === 200) {
            const data = result.data.popularPrompts || [];
            if (data.length > 0) {
                container.innerHTML = data.map(prompt => `
                    <div class="prompt-item">
                        <div class="prompt-text">${escapeHtml(prompt.text)}</div>
                        <div class="prompt-stats">
                            <span>使用次数: ${prompt.count}</span>
                            <span>成功率: ${prompt.successRate}%</span>
                        </div>
                    </div>
                `).join('');
            } else {
                container.innerHTML = '<div class="loading"><p>暂无热门提示词数据</p></div>';
            }
        } else {
            container.innerHTML = '<div class="loading"><p>暂无热门提示词数据</p></div>';
        }
        
    } catch (error) {
        console.error('加载热门提示词失败:', error);
        const container = document.getElementById('popular-prompts');
        container.innerHTML = '<div class="loading"><p>加载失败，请稍后重试</p></div>';
    }
}

// 加载指标图表
async function loadMetricsChart(days) {
    try {
        const response = await fetch(`/admin/dashboard/metrics?days=${days}`);
        if (!response.ok) {
            throw new Error('获取指标数据失败');
        }
        
        const data = await response.json();
        const container = document.getElementById('metrics-chart');
        
        if (data && data.length > 0) {
            // 这里可以集成图表库（如Chart.js）来显示数据
            // 暂时显示简单的数据列表
            container.innerHTML = `
                <div class="metrics-summary">
                    <h4>最近${days}天系统指标</h4>
                    <div class="metrics-list">
                        ${data.map(metric => `
                            <div class="metric-item">
                                <span class="metric-date">${metric.date}</span>
                                <span class="metric-value">任务数: ${metric.taskCount}</span>
                                <span class="metric-value">成功率: ${metric.successRate}%</span>
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        } else {
            container.innerHTML = '<div class="loading"><p>暂无指标数据</p></div>';
        }
        
    } catch (error) {
        console.error('加载指标数据失败:', error);
        const container = document.getElementById('metrics-chart');
        container.innerHTML = '<div class="loading"><p>加载失败，请稍后重试</p></div>';
    }
}

// 管理员工具函数
function updateElement(id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = value;
    }
}

function updateHealthStatus(id, status) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = status.message || status.status || '未知';
        element.className = 'health-status';
        
        if (status.status === 'healthy') {
            element.classList.add('healthy');
        } else if (status.status === 'warning') {
            element.classList.add('warning');
        } else if (status.status === 'error') {
            element.classList.add('error');
        } else {
            element.classList.add('unknown');
        }
        
        // 设置title属性显示详细信息
        if (status.details) {
            element.title = status.details;
        }
        
        // 添加点击事件显示详细信息弹窗
        element.onclick = () => showHealthStatusModal(id, status);
        
        // 存储状态数据供弹窗使用
        element.dataset.healthStatus = JSON.stringify(status);
    }
}

function showAdminLoadingState() {
    // 重置所有数据显示为加载状态
    updateElement('today-tasks', '加载中...');
    updateElement('success-rate', '加载中...');
    updateElement('avg-time', '加载中...');
    updateElement('avg-rating', '加载中...');
}

function showAdminErrorState(message) {
    console.error(message);
    // 可以在这里显示全局错误提示
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 健康状态弹窗相关函数
function showHealthStatusModal(elementId, status) {
    const modal = document.getElementById('health-modal-overlay');
    const title = document.getElementById('health-modal-title');
    const label = document.getElementById('health-modal-label');
    const statusElement = document.getElementById('health-modal-status');
    const details = document.getElementById('health-modal-details');
    const suggestions = document.getElementById('health-modal-suggestions');
    const suggestionsList = document.getElementById('health-modal-suggestions-list');
    
    // 获取健康状态类型的中文名称
    const statusTypeMap = {
        'db-status': '数据库连接',
        'ai-status': 'AI服务状态',
        'storage-status': '存储空间'
    };
    
    // 获取状态的中文描述
    const statusTextMap = {
        'healthy': '正常',
        'warning': '警告',
        'error': '错误',
        'unknown': '未知'
    };
    
    // 设置弹窗内容
    title.textContent = `${statusTypeMap[elementId] || '系统状态'} - 详细信息`;
    label.textContent = statusTypeMap[elementId] || '状态类型';
    
    // 设置状态显示
    const statusText = statusTextMap[status.status] || status.status || '未知';
    statusElement.textContent = statusText;
    statusElement.className = 'health-detail-status';
    
    if (status.status === 'healthy') {
        statusElement.style.background = '#28a745';
        statusElement.style.color = 'white';
    } else if (status.status === 'warning') {
        statusElement.style.background = '#ffc107';
        statusElement.style.color = '#856404';
    } else if (status.status === 'error') {
        statusElement.style.background = '#dc3545';
        statusElement.style.color = 'white';
    } else {
        statusElement.style.background = '#6c757d';
        statusElement.style.color = 'white';
    }
    
    // 设置详细信息
    let detailsText = status.details || status.message || '暂无详细信息';
    
    // 根据不同的状态类型和状态，提供更详细的信息
    if (elementId === 'storage-status' && status.status === 'warning') {
        detailsText = status.details || `存储空间使用率较高，当前使用率: ${status.usage || '未知'}`;
    } else if (elementId === 'db-status' && status.status === 'error') {
        detailsText = status.details || '数据库连接失败，请检查数据库服务是否正常运行';
    } else if (elementId === 'ai-status' && status.status === 'error') {
        detailsText = status.details || 'AI服务连接失败，请检查AI服务配置和网络连接';
    }
    
    details.textContent = detailsText;
    
    // 设置建议操作
    const suggestionMap = {
        'db-status': {
            'error': [
                '检查数据库服务是否正常运行',
                '验证数据库连接配置',
                '检查网络连接',
                '查看数据库日志获取更多信息'
            ],
            'warning': [
                '监控数据库性能指标',
                '检查数据库连接池配置',
                '优化数据库查询'
            ]
        },
        'ai-status': {
            'error': [
                '检查AI服务配置',
                '验证API密钥和端点',
                '检查网络连接',
                '联系AI服务提供商'
            ],
            'warning': [
                '监控AI服务响应时间',
                '检查API调用频率限制',
                '优化请求参数'
            ]
        },
        'storage-status': {
            'warning': [
                '清理不必要的文件',
                '扩展存储容量',
                '设置文件自动清理策略',
                '监控存储使用趋势'
            ],
            'error': [
                '立即清理存储空间',
                '紧急扩展存储容量',
                '停止非关键服务',
                '联系系统管理员'
            ]
        }
    };
    
    const currentSuggestions = suggestionMap[elementId]?.[status.status];
    if (currentSuggestions && currentSuggestions.length > 0) {
        suggestions.style.display = 'block';
        suggestionsList.innerHTML = currentSuggestions.map(suggestion => 
            `<li>${suggestion}</li>`
        ).join('');
    } else {
        suggestions.style.display = 'none';
    }
    
    // 显示弹窗
    modal.style.display = 'flex';
}

function hideHealthStatusModal() {
    const modal = document.getElementById('health-modal-overlay');
    modal.style.display = 'none';
}

// 初始化弹窗事件监听器
function initHealthStatusModal() {
    const modal = document.getElementById('health-modal-overlay');
    const closeButton = document.getElementById('health-modal-close');
    
    // 点击关闭按钮
    closeButton.addEventListener('click', hideHealthStatusModal);
    
    // 点击遮罩层关闭弹窗
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            hideHealthStatusModal();
        }
    });
    
    // ESC键关闭弹窗
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.style.display === 'flex') {
            hideHealthStatusModal();
        }
    });
}