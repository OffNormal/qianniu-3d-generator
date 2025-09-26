// 全局变量
let currentTaskId = null;
let statusCheckInterval = null;

// DOM 元素
const elements = {
    // 选项卡
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
    
    // 结果按钮
    previewBtn: document.getElementById('preview-btn'),
    downloadBtn: document.getElementById('download-btn'),
    
    // 预览模态框
    previewModal: document.getElementById('preview-modal'),
    closePreview: document.getElementById('close-preview'),
    modelPreviewImg: document.getElementById('model-preview-img'),
    previewControlBtns: document.querySelectorAll('.preview-control-btn'),
    
    // 加载覆盖层
    loadingOverlay: document.getElementById('loading-overlay')
};

// 初始化应用
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

function initializeApp() {
    setupTabSwitching();
    setupFormHandlers();
    setupFileUpload();
    setupCharacterCounters();
    setupEventListeners();
    
    console.log('3D模型生成器已初始化');
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
    const requestData = {
        text: formData.get('text'),
        complexity: formData.get('complexity'),
        format: formData.get('format')
    };
    
    // 验证输入
    if (!requestData.text.trim()) {
        showError('请输入模型描述');
        return;
    }
    
    if (requestData.text.length > 1000) {
        showError('文本描述不能超过1000个字符');
        return;
    }
    
    try {
        showLoading(true);
        
        const response = await fetch('/api/v1/models/generate/text', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            currentTaskId = result.data.taskId;
            showStatusPanel(result.data);
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
        
        const response = await fetch('/api/v1/models/generate/image', {
            method: 'POST',
            body: formData
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            currentTaskId = result.data.taskId;
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

// 文件上传处理
function setupFileUpload() {
    const fileInput = elements.imageInput;
    const uploadArea = elements.fileUploadArea;
    
    // 点击上传
    uploadArea.addEventListener('click', () => {
        if (!elements.imagePreview.style.display || elements.imagePreview.style.display === 'none') {
            fileInput.click();
        }
    });
    
    // 文件选择
    fileInput.addEventListener('change', handleFileSelect);
    
    // 拖拽上传
    uploadArea.addEventListener('dragover', handleDragOver);
    uploadArea.addEventListener('dragleave', handleDragLeave);
    uploadArea.addEventListener('drop', handleFileDrop);
    
    // 移除图片
    elements.removeImage.addEventListener('click', removeSelectedImage);
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
    
    // 预览控制按钮
    elements.previewControlBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const angle = btn.dataset.angle;
            changePreviewAngle(angle);
            
            // 更新按钮状态
            elements.previewControlBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
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
    elements.currentTaskId.textContent = taskData.taskId;
    elements.currentStatus.textContent = taskData.status;
    elements.currentStatus.className = `status-badge ${taskData.status.toLowerCase()}`;
    
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
    elements.currentStatus.textContent = statusData.status;
    elements.currentStatus.className = `status-badge ${statusData.status.toLowerCase()}`;
    
    elements.progressFill.style.width = `${statusData.progress}%`;
    elements.progressPercent.textContent = `${statusData.progress}%`;
    
    // 更新状态消息
    const statusMessages = {
        'PENDING': '任务已创建，等待处理...',
        'PROCESSING': '正在生成3D模型...',
        'COMPLETED': '模型生成完成！',
        'FAILED': '生成失败，请重试'
    };
    
    elements.statusMessage.textContent = statusMessages[statusData.status] || statusData.errorMessage || '处理中...';
    
    // 如果完成，显示结果面板
    if (statusData.status === 'COMPLETED') {
        showResultPanel(statusData);
        stopStatusPolling();
    } else if (statusData.status === 'FAILED') {
        stopStatusPolling();
    }
}

function showResultPanel(statusData) {
    elements.resultPanel.style.display = 'block';
    
    // 更新模型信息
    if (statusData.modelResult) {
        const modelInfo = statusData.modelResult.modelInfo;
        if (modelInfo) {
            elements.modelInfo.innerHTML = `
                <div><strong>模型信息:</strong></div>
                <div>顶点数: ${modelInfo.vertices || 'N/A'}</div>
                <div>面数: ${modelInfo.faces || 'N/A'}</div>
                <div>文件大小: ${modelInfo.fileSize || 'N/A'}</div>
                <div>格式: ${modelInfo.format || 'N/A'}</div>
            `;
        }
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
        const response = await fetch(`/api/v1/models/status/${currentTaskId}`);
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
        
        const previewUrl = `/api/v1/models/preview/${currentTaskId}?angle=front&size=medium`;
        elements.modelPreviewImg.src = previewUrl;
        elements.previewModal.style.display = 'flex';
        
        // 重置预览控制按钮
        elements.previewControlBtns.forEach(btn => btn.classList.remove('active'));
        elements.previewControlBtns[0].classList.add('active'); // 默认选中第一个
        
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

function changePreviewAngle(angle) {
    if (!currentTaskId) return;
    
    const previewUrl = `/api/v1/models/preview/${currentTaskId}?angle=${angle}&size=medium`;
    elements.modelPreviewImg.src = previewUrl;
}

// 模型下载
function downloadModel() {
    if (!currentTaskId) return;
    
    const downloadUrl = `/api/v1/models/download/${currentTaskId}`;
    
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