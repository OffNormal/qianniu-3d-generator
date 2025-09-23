/**
 * 性能优化工具类
 * 包含资源预加载、懒加载、缓存管理等功能
 */
class PerformanceOptimizer {
    constructor() {
        this.cache = new Map();
        this.preloadQueue = [];
        this.lazyLoadObserver = null;
        this.init();
    }

    init() {
        this.setupLazyLoading();
        this.setupResourcePreloading();
        this.setupCacheManagement();
        this.setupPerformanceMonitoring();
    }

    /**
     * 设置懒加载
     */
    setupLazyLoading() {
        if ('IntersectionObserver' in window) {
            this.lazyLoadObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const element = entry.target;
                        
                        // 懒加载图片
                        if (element.tagName === 'IMG' && element.dataset.src) {
                            element.src = element.dataset.src;
                            element.removeAttribute('data-src');
                            this.lazyLoadObserver.unobserve(element);
                        }
                        
                        // 懒加载内容区域
                        if (element.classList.contains('lazy-content')) {
                            this.loadLazyContent(element);
                            this.lazyLoadObserver.unobserve(element);
                        }
                    }
                });
            }, {
                rootMargin: '50px 0px',
                threshold: 0.1
            });

            // 观察所有懒加载元素
            document.querySelectorAll('[data-src], .lazy-content').forEach(el => {
                this.lazyLoadObserver.observe(el);
            });
        }
    }

    /**
     * 设置资源预加载
     */
    setupResourcePreloading() {
        // 预加载关键CSS
        this.preloadResource('/css/responsive.css', 'style');
        
        // 预加载关键字体
        this.preloadResource('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/webfonts/fa-solid-900.woff2', 'font');
        
        // 预加载下一页面的资源
        this.preloadNextPageResources();
    }

    /**
     * 预加载资源
     */
    preloadResource(href, as, type = 'preload') {
        const link = document.createElement('link');
        link.rel = type;
        link.as = as;
        link.href = href;
        if (as === 'font') {
            link.crossOrigin = 'anonymous';
        }
        document.head.appendChild(link);
    }

    /**
     * 预加载下一页面资源
     */
    preloadNextPageResources() {
        const currentPath = window.location.pathname;
        const nextPages = {
            '/': ['/workspace', '/history'],
            '/workspace': ['/history', '/profile'],
            '/history': ['/workspace', '/api-docs'],
            '/profile': ['/workspace', '/api-docs'],
            '/api-docs': ['/workspace', '/profile']
        };

        if (nextPages[currentPath]) {
            nextPages[currentPath].forEach(page => {
                this.preloadResource(page, 'document', 'prefetch');
            });
        }
    }

    /**
     * 设置缓存管理
     */
    setupCacheManagement() {
        // 检查浏览器支持
        if ('caches' in window) {
            this.setupServiceWorkerCache();
        }
        
        // 设置本地存储缓存
        this.setupLocalStorageCache();
    }

    /**
     * 设置Service Worker缓存
     */
    setupServiceWorkerCache() {
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('/sw.js')
                .then(registration => {
                    console.log('Service Worker registered:', registration);
                })
                .catch(error => {
                    console.log('Service Worker registration failed:', error);
                });
        }
    }

    /**
     * 设置本地存储缓存
     */
    setupLocalStorageCache() {
        // 缓存API响应
        this.cacheApiResponse = (key, data, ttl = 300000) => { // 5分钟TTL
            const cacheData = {
                data: data,
                timestamp: Date.now(),
                ttl: ttl
            };
            try {
                localStorage.setItem(`cache_${key}`, JSON.stringify(cacheData));
            } catch (e) {
                console.warn('LocalStorage cache failed:', e);
            }
        };

        // 获取缓存的API响应
        this.getCachedApiResponse = (key) => {
            try {
                const cached = localStorage.getItem(`cache_${key}`);
                if (cached) {
                    const cacheData = JSON.parse(cached);
                    if (Date.now() - cacheData.timestamp < cacheData.ttl) {
                        return cacheData.data;
                    } else {
                        localStorage.removeItem(`cache_${key}`);
                    }
                }
            } catch (e) {
                console.warn('LocalStorage cache retrieval failed:', e);
            }
            return null;
        };
    }

    /**
     * 设置性能监控
     */
    setupPerformanceMonitoring() {
        // 监控页面加载性能
        window.addEventListener('load', () => {
            setTimeout(() => {
                const perfData = performance.getEntriesByType('navigation')[0];
                if (perfData) {
                    console.log('Page Load Performance:', {
                        domContentLoaded: perfData.domContentLoadedEventEnd - perfData.domContentLoadedEventStart,
                        loadComplete: perfData.loadEventEnd - perfData.loadEventStart,
                        totalTime: perfData.loadEventEnd - perfData.fetchStart
                    });
                }
            }, 0);
        });

        // 监控资源加载性能
        this.monitorResourcePerformance();
    }

    /**
     * 监控资源加载性能
     */
    monitorResourcePerformance() {
        const observer = new PerformanceObserver((list) => {
            list.getEntries().forEach((entry) => {
                if (entry.duration > 1000) { // 超过1秒的资源
                    console.warn('Slow resource detected:', entry.name, entry.duration + 'ms');
                }
            });
        });
        observer.observe({ entryTypes: ['resource'] });
    }

    /**
     * 懒加载内容
     */
    loadLazyContent(element) {
        const contentType = element.dataset.contentType;
        const contentUrl = element.dataset.contentUrl;

        if (contentType && contentUrl) {
            // 检查缓存
            const cached = this.getCachedApiResponse(contentUrl);
            if (cached) {
                this.renderContent(element, cached);
                return;
            }

            // 显示加载状态
            element.innerHTML = '<div class="loading-spinner">加载中...</div>';

            // 加载内容
            fetch(contentUrl)
                .then(response => response.json())
                .then(data => {
                    this.cacheApiResponse(contentUrl, data);
                    this.renderContent(element, data);
                })
                .catch(error => {
                    console.error('Lazy content loading failed:', error);
                    element.innerHTML = '<div class="error-message">加载失败，请重试</div>';
                });
        }
    }

    /**
     * 渲染内容
     */
    renderContent(element, data) {
        // 根据内容类型渲染
        const contentType = element.dataset.contentType;
        switch (contentType) {
            case 'model-list':
                this.renderModelList(element, data);
                break;
            case 'statistics':
                this.renderStatistics(element, data);
                break;
            default:
                element.innerHTML = JSON.stringify(data);
        }
    }

    /**
     * 渲染模型列表
     */
    renderModelList(element, data) {
        const html = data.models.map(model => `
            <div class="model-item">
                <img src="${model.thumbnail}" alt="${model.name}" loading="lazy">
                <h3>${model.name}</h3>
                <p>${model.description}</p>
            </div>
        `).join('');
        element.innerHTML = html;
    }

    /**
     * 渲染统计信息
     */
    renderStatistics(element, data) {
        const html = `
            <div class="stats-grid">
                <div class="stat-item">
                    <span class="stat-value">${data.totalModels}</span>
                    <span class="stat-label">总模型数</span>
                </div>
                <div class="stat-item">
                    <span class="stat-value">${data.successRate}%</span>
                    <span class="stat-label">成功率</span>
                </div>
                <div class="stat-item">
                    <span class="stat-value">${data.avgTime}s</span>
                    <span class="stat-label">平均生成时间</span>
                </div>
            </div>
        `;
        element.innerHTML = html;
    }

    /**
     * 图片压缩
     */
    compressImage(file, quality = 0.8, maxWidth = 1920, maxHeight = 1080) {
        return new Promise((resolve) => {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            const img = new Image();

            img.onload = () => {
                // 计算新尺寸
                let { width, height } = img;
                if (width > maxWidth || height > maxHeight) {
                    const ratio = Math.min(maxWidth / width, maxHeight / height);
                    width *= ratio;
                    height *= ratio;
                }

                canvas.width = width;
                canvas.height = height;

                // 绘制并压缩
                ctx.drawImage(img, 0, 0, width, height);
                canvas.toBlob(resolve, 'image/jpeg', quality);
            };

            img.src = URL.createObjectURL(file);
        });
    }

    /**
     * 防抖函数
     */
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

    /**
     * 节流函数
     */
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

    /**
     * 清理缓存
     */
    clearCache() {
        // 清理过期的localStorage缓存
        Object.keys(localStorage).forEach(key => {
            if (key.startsWith('cache_')) {
                try {
                    const cached = JSON.parse(localStorage.getItem(key));
                    if (Date.now() - cached.timestamp > cached.ttl) {
                        localStorage.removeItem(key);
                    }
                } catch (e) {
                    localStorage.removeItem(key);
                }
            }
        });
    }
}

// 初始化性能优化器
document.addEventListener('DOMContentLoaded', () => {
    window.performanceOptimizer = new PerformanceOptimizer();
    
    // 定期清理缓存
    setInterval(() => {
        window.performanceOptimizer.clearCache();
    }, 600000); // 每10分钟清理一次
});

// 导出给其他模块使用
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PerformanceOptimizer;
}