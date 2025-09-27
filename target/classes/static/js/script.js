// 全局变量
let currentSection = 'welcome';

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeNavigation();
    initializeAdminDashboard();
    
    // 默认显示欢迎页面
    showSection('welcome');
});

// 初始化导航功能
function initializeNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const target = this.getAttribute('href').substring(1);
            showSection(target);
            
            // 更新导航状态
            navLinks.forEach(l => l.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

// 显示指定的页面部分
function showSection(sectionName) {
    // 隐藏所有section
    const sections = document.querySelectorAll('section');
    sections.forEach(section => {
        section.style.display = 'none';
    });
    
    // 显示目标section
    let targetSection;
    switch(sectionName) {
        case 'welcome':
            targetSection = document.querySelector('.welcome-section');
            break;
        case 'history':
            targetSection = document.querySelector('.history-section');
            break;
        case 'admin':
            targetSection = document.querySelector('.admin-section');
            if (targetSection) {
                loadAdminDashboard();
            }
            break;
        default:
            targetSection = document.querySelector('.welcome-section');
    }
    
    if (targetSection) {
        targetSection.style.display = 'block';
        currentSection = sectionName;
    }
}

// 初始化管理员仪表板 - 调用app.js中的实现
function initializeAdminDashboard() {
    // 调用app.js中的管理员仪表板初始化函数
    if (typeof window.initializeAdminDashboard === 'function') {
        window.initializeAdminDashboard();
    }
}

// 加载管理员仪表板数据 - 调用app.js中的实现
async function loadAdminDashboard() {
    // 调用app.js中的管理员仪表板加载函数
    if (typeof window.loadAdminDashboard === 'function') {
        await window.loadAdminDashboard();
    }
}

// 导出函数供全局使用
window.showSection = showSection;
window.loadAdminDashboard = loadAdminDashboard;