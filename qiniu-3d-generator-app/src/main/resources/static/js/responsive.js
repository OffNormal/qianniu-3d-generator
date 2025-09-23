/**
 * 响应式设计和移动端交互脚本
 */

class ResponsiveManager {
    constructor() {
        this.isMobile = window.innerWidth <= 768;
        this.isTablet = window.innerWidth <= 1024 && window.innerWidth > 768;
        this.isDesktop = window.innerWidth > 1024;
        
        this.init();
        this.bindEvents();
    }

    init() {
        this.createMobileNavigation();
        this.createTouchInteractions();
        this.optimizeForMobile();
        this.handleOrientationChange();
    }

    bindEvents() {
        // 窗口大小变化
        window.addEventListener('resize', this.debounce(() => {
            this.updateBreakpoints();
            this.handleResize();
        }, 250));

        // 方向变化
        window.addEventListener('orientationchange', () => {
            setTimeout(() => {
                this.handleOrientationChange();
            }, 100);
        });

        // 触摸事件
        if ('ontouchstart' in window) {
            this.bindTouchEvents();
        }
    }

    createMobileNavigation() {
        const navbar = document.querySelector('.navbar');
        if (!navbar) return;

        // 创建移动端菜单按钮
        const mobileToggle = document.createElement('button');
        mobileToggle.className = 'mobile-nav-toggle';
        mobileToggle.innerHTML = '<i class="fas fa-bars"></i>';
        mobileToggle.setAttribute('aria-label', '打开导航菜单');

        // 创建遮罩层
        const overlay = document.createElement('div');
        overlay.className = 'mobile-nav-overlay';

        // 插入到导航栏
        const navContainer = navbar.querySelector('.nav-container');
        if (navContainer) {
            navContainer.appendChild(mobileToggle);
        }
        document.body.appendChild(overlay);

        // 绑定事件
        mobileToggle.addEventListener('click', () => {
            this.toggleMobileNav();
        });

        overlay.addEventListener('click', () => {
            this.closeMobileNav();
        });

        // ESC键关闭菜单
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.closeMobileNav();
            }
        });

        // 添加关闭按钮到移动菜单
        const navLinks = document.querySelector('.nav-links');
        if (navLinks) {
            const closeBtn = document.createElement('button');
            closeBtn.className = 'mobile-nav-close';
            closeBtn.innerHTML = '<i class="fas fa-times"></i>';
            closeBtn.style.cssText = `
                position: absolute;
                top: 1rem;
                right: 1rem;
                background: none;
                border: none;
                font-size: 1.5rem;
                color: #333;
                cursor: pointer;
                padding: 0.5rem;
                border-radius: 8px;
                transition: background-color 0.2s;
            `;
            
            closeBtn.addEventListener('click', () => {
                this.closeMobileNav();
            });
            
            navLinks.insertBefore(closeBtn, navLinks.firstChild);
        }
    }

    toggleMobileNav() {
        const navLinks = document.querySelector('.nav-links');
        const overlay = document.querySelector('.mobile-nav-overlay');
        const toggle = document.querySelector('.mobile-nav-toggle');

        if (navLinks && overlay && toggle) {
            const isActive = navLinks.classList.contains('active');
            
            if (isActive) {
                this.closeMobileNav();
            } else {
                navLinks.classList.add('active');
                overlay.classList.add('active');
                toggle.innerHTML = '<i class="fas fa-times"></i>';
                toggle.setAttribute('aria-label', '关闭导航菜单');
                document.body.style.overflow = 'hidden';
            }
        }
    }

    closeMobileNav() {
        const navLinks = document.querySelector('.nav-links');
        const overlay = document.querySelector('.mobile-nav-overlay');
        const toggle = document.querySelector('.mobile-nav-toggle');

        if (navLinks && overlay && toggle) {
            navLinks.classList.remove('active');
            overlay.classList.remove('active');
            toggle.innerHTML = '<i class="fas fa-bars"></i>';
            toggle.setAttribute('aria-label', '打开导航菜单');
            document.body.style.overflow = '';
        }
    }

    createTouchInteractions() {
        // 为所有按钮添加触摸反馈
        const buttons = document.querySelectorAll('.btn, button');
        buttons.forEach(btn => {
            if (!btn.classList.contains('btn-touch')) {
                btn.classList.add('btn-touch');
            }
        });

        // 为输入框添加触摸优化
        const inputs = document.querySelectorAll('input, textarea, select');
        inputs.forEach(input => {
            if (!input.classList.contains('input-touch')) {
                input.classList.add('input-touch');
            }
        });
    }

    bindTouchEvents() {
        // 添加触摸滑动支持
        const swipeContainers = document.querySelectorAll('.swipe-container');
        swipeContainers.forEach(container => {
            this.addSwipeSupport(container);
        });

        // 添加长按支持
        const longPressElements = document.querySelectorAll('[data-long-press]');
        longPressElements.forEach(element => {
            this.addLongPressSupport(element);
        });
    }

    addSwipeSupport(container) {
        let startX = 0;
        let startY = 0;
        let isScrolling = false;

        container.addEventListener('touchstart', (e) => {
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;
            isScrolling = false;
        }, { passive: true });

        container.addEventListener('touchmove', (e) => {
            if (!startX || !startY) return;

            const diffX = Math.abs(e.touches[0].clientX - startX);
            const diffY = Math.abs(e.touches[0].clientY - startY);

            if (diffY > diffX) {
                isScrolling = true;
            }
        }, { passive: true });

        container.addEventListener('touchend', (e) => {
            if (isScrolling) return;

            const endX = e.changedTouches[0].clientX;
            const diffX = startX - endX;

            if (Math.abs(diffX) > 50) {
                const event = new CustomEvent('swipe', {
                    detail: {
                        direction: diffX > 0 ? 'left' : 'right',
                        distance: Math.abs(diffX)
                    }
                });
                container.dispatchEvent(event);
            }

            startX = 0;
            startY = 0;
        }, { passive: true });
    }

    addLongPressSupport(element) {
        let pressTimer = null;

        element.addEventListener('touchstart', (e) => {
            pressTimer = setTimeout(() => {
                const event = new CustomEvent('longpress', {
                    detail: { originalEvent: e }
                });
                element.dispatchEvent(event);
            }, 500);
        });

        element.addEventListener('touchend', () => {
            if (pressTimer) {
                clearTimeout(pressTimer);
                pressTimer = null;
            }
        });

        element.addEventListener('touchmove', () => {
            if (pressTimer) {
                clearTimeout(pressTimer);
                pressTimer = null;
            }
        });
    }

    optimizeForMobile() {
        if (!this.isMobile) return;

        // 优化表格显示
        this.optimizeTables();
        
        // 优化模态框
        this.optimizeModals();
        
        // 添加返回顶部按钮
        this.addBackToTop();
        
        // 优化表单
        this.optimizeForms();
    }

    optimizeTables() {
        const tables = document.querySelectorAll('table');
        tables.forEach(table => {
            const wrapper = table.parentElement;
            if (!wrapper.classList.contains('table-responsive')) {
                const responsiveWrapper = document.createElement('div');
                responsiveWrapper.className = 'table-responsive';
                table.parentElement.insertBefore(responsiveWrapper, table);
                responsiveWrapper.appendChild(table);
            }

            // 创建卡片式布局
            this.createTableCards(table);
        });
    }

    createTableCards(table) {
        const headers = Array.from(table.querySelectorAll('thead th')).map(th => th.textContent.trim());
        const rows = Array.from(table.querySelectorAll('tbody tr'));

        const cardsContainer = document.createElement('div');
        cardsContainer.className = 'table-cards';

        rows.forEach(row => {
            const cells = Array.from(row.querySelectorAll('td'));
            const card = document.createElement('div');
            card.className = 'card';

            cells.forEach((cell, index) => {
                if (headers[index]) {
                    const cardRow = document.createElement('div');
                    cardRow.className = 'card-row';
                    
                    const label = document.createElement('span');
                    label.className = 'card-label';
                    label.textContent = headers[index];
                    
                    const value = document.createElement('span');
                    value.className = 'card-value';
                    value.innerHTML = cell.innerHTML;
                    
                    cardRow.appendChild(label);
                    cardRow.appendChild(value);
                    card.appendChild(cardRow);
                }
            });

            cardsContainer.appendChild(card);
        });

        table.parentElement.appendChild(cardsContainer);

        // 添加切换按钮
        const toggleBtn = document.createElement('button');
        toggleBtn.className = 'btn btn-outline';
        toggleBtn.textContent = '切换视图';
        toggleBtn.style.marginBottom = '1rem';
        
        toggleBtn.addEventListener('click', () => {
            table.style.display = table.style.display === 'none' ? '' : 'none';
            cardsContainer.classList.toggle('active');
        });

        table.parentElement.insertBefore(toggleBtn, table);
    }

    optimizeModals() {
        const modals = document.querySelectorAll('.modal');
        modals.forEach(modal => {
            if (this.isMobile) {
                modal.classList.add('modal-mobile');
            }
        });
    }

    addBackToTop() {
        const backToTop = document.createElement('button');
        backToTop.className = 'back-to-top';
        backToTop.innerHTML = '<i class="fas fa-arrow-up"></i>';
        backToTop.style.cssText = `
            position: fixed;
            bottom: 6rem;
            right: 2rem;
            width: 50px;
            height: 50px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 50%;
            font-size: 1.2rem;
            cursor: pointer;
            box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
            transition: all 0.3s;
            opacity: 0;
            visibility: hidden;
            z-index: 1000;
        `;

        document.body.appendChild(backToTop);

        // 滚动显示/隐藏
        window.addEventListener('scroll', this.throttle(() => {
            if (window.pageYOffset > 300) {
                backToTop.style.opacity = '1';
                backToTop.style.visibility = 'visible';
            } else {
                backToTop.style.opacity = '0';
                backToTop.style.visibility = 'hidden';
            }
        }, 100));

        // 点击返回顶部
        backToTop.addEventListener('click', () => {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }

    optimizeForms() {
        const forms = document.querySelectorAll('form');
        forms.forEach(form => {
            // 添加表单验证提示
            const inputs = form.querySelectorAll('input, textarea, select');
            inputs.forEach(input => {
                input.addEventListener('invalid', (e) => {
                    e.preventDefault();
                    this.showValidationMessage(input);
                });
            });
        });
    }

    showValidationMessage(input) {
        const message = input.validationMessage;
        if (!message) return;

        // 移除现有提示
        const existingTip = input.parentElement.querySelector('.validation-tip');
        if (existingTip) {
            existingTip.remove();
        }

        // 创建新提示
        const tip = document.createElement('div');
        tip.className = 'validation-tip';
        tip.textContent = message;
        tip.style.cssText = `
            color: #dc3545;
            font-size: 0.875rem;
            margin-top: 0.25rem;
            padding: 0.5rem;
            background: #f8d7da;
            border: 1px solid #f5c6cb;
            border-radius: 4px;
        `;

        input.parentElement.appendChild(tip);

        // 3秒后自动移除
        setTimeout(() => {
            if (tip.parentElement) {
                tip.remove();
            }
        }, 3000);
    }

    updateBreakpoints() {
        this.isMobile = window.innerWidth <= 768;
        this.isTablet = window.innerWidth <= 1024 && window.innerWidth > 768;
        this.isDesktop = window.innerWidth > 1024;
    }

    handleResize() {
        // 关闭移动菜单
        if (!this.isMobile) {
            this.closeMobileNav();
        }

        // 重新优化布局
        if (this.isMobile) {
            this.optimizeForMobile();
        }
    }

    handleOrientationChange() {
        // 延迟处理，等待浏览器完成方向变化
        setTimeout(() => {
            this.updateBreakpoints();
            this.handleResize();
            
            // 重新计算视口高度
            const vh = window.innerHeight * 0.01;
            document.documentElement.style.setProperty('--vh', `${vh}px`);
        }, 100);
    }

    // 工具函数
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    new ResponsiveManager();
});

// 导出供其他脚本使用
window.ResponsiveManager = ResponsiveManager;