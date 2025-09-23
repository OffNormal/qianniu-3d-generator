/**
 * Service Worker for 七牛云3D模型生成应用
 * 提供离线缓存和性能优化
 */

const CACHE_NAME = 'qiniu-3d-generator-v1.0.0';
const STATIC_CACHE_NAME = 'qiniu-3d-static-v1.0.0';
const API_CACHE_NAME = 'qiniu-3d-api-v1.0.0';

// 需要缓存的静态资源
const STATIC_ASSETS = [
    '/',
    '/workspace',
    '/history',
    '/profile',
    '/api-docs',
    '/css/responsive.css',
    '/js/responsive.js',
    '/js/performance.js',
    'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css'
];

// 需要缓存的API端点
const API_ENDPOINTS = [
    '/api/models',
    '/api/history',
    '/api/user/profile',
    '/api/statistics'
];

// 安装事件 - 预缓存静态资源
self.addEventListener('install', (event) => {
    console.log('Service Worker installing...');
    
    event.waitUntil(
        Promise.all([
            // 缓存静态资源
            caches.open(STATIC_CACHE_NAME).then((cache) => {
                console.log('Caching static assets...');
                return cache.addAll(STATIC_ASSETS);
            }),
            // 跳过等待，立即激活
            self.skipWaiting()
        ])
    );
});

// 激活事件 - 清理旧缓存
self.addEventListener('activate', (event) => {
    console.log('Service Worker activating...');
    
    event.waitUntil(
        Promise.all([
            // 清理旧缓存
            caches.keys().then((cacheNames) => {
                return Promise.all(
                    cacheNames.map((cacheName) => {
                        if (cacheName !== CACHE_NAME && 
                            cacheName !== STATIC_CACHE_NAME && 
                            cacheName !== API_CACHE_NAME) {
                            console.log('Deleting old cache:', cacheName);
                            return caches.delete(cacheName);
                        }
                    })
                );
            }),
            // 立即控制所有客户端
            self.clients.claim()
        ])
    );
});

// 拦截网络请求
self.addEventListener('fetch', (event) => {
    const request = event.request;
    const url = new URL(request.url);

    // 只处理GET请求
    if (request.method !== 'GET') {
        return;
    }

    // 处理静态资源
    if (isStaticAsset(url)) {
        event.respondWith(handleStaticAsset(request));
        return;
    }

    // 处理API请求
    if (isApiRequest(url)) {
        event.respondWith(handleApiRequest(request));
        return;
    }

    // 处理页面请求
    if (isPageRequest(url)) {
        event.respondWith(handlePageRequest(request));
        return;
    }

    // 其他请求直接通过网络
    event.respondWith(fetch(request));
});

/**
 * 判断是否为静态资源
 */
function isStaticAsset(url) {
    const staticExtensions = ['.css', '.js', '.png', '.jpg', '.jpeg', '.gif', '.svg', '.woff', '.woff2', '.ttf'];
    return staticExtensions.some(ext => url.pathname.endsWith(ext)) ||
           url.hostname === 'cdnjs.cloudflare.com';
}

/**
 * 判断是否为API请求
 */
function isApiRequest(url) {
    return url.pathname.startsWith('/api/');
}

/**
 * 判断是否为页面请求
 */
function isPageRequest(url) {
    return url.pathname === '/' || 
           url.pathname.startsWith('/workspace') ||
           url.pathname.startsWith('/history') ||
           url.pathname.startsWith('/profile') ||
           url.pathname.startsWith('/api-docs');
}

/**
 * 处理静态资源请求 - 缓存优先策略
 */
async function handleStaticAsset(request) {
    try {
        // 先从缓存查找
        const cache = await caches.open(STATIC_CACHE_NAME);
        const cachedResponse = await cache.match(request);
        
        if (cachedResponse) {
            // 后台更新缓存
            updateCacheInBackground(request, cache);
            return cachedResponse;
        }

        // 缓存中没有，从网络获取
        const networkResponse = await fetch(request);
        
        if (networkResponse.ok) {
            // 缓存成功的响应
            cache.put(request, networkResponse.clone());
        }
        
        return networkResponse;
    } catch (error) {
        console.error('Static asset fetch failed:', error);
        
        // 返回离线页面或默认资源
        if (request.destination === 'document') {
            return caches.match('/offline.html');
        }
        
        throw error;
    }
}

/**
 * 处理API请求 - 网络优先策略
 */
async function handleApiRequest(request) {
    const cache = await caches.open(API_CACHE_NAME);
    
    try {
        // 先尝试网络请求
        const networkResponse = await fetch(request);
        
        if (networkResponse.ok) {
            // 只缓存GET请求的成功响应
            if (request.method === 'GET') {
                // 设置缓存过期时间（5分钟）
                const responseToCache = networkResponse.clone();
                const headers = new Headers(responseToCache.headers);
                headers.set('sw-cache-timestamp', Date.now().toString());
                
                const cachedResponse = new Response(responseToCache.body, {
                    status: responseToCache.status,
                    statusText: responseToCache.statusText,
                    headers: headers
                });
                
                cache.put(request, cachedResponse);
            }
        }
        
        return networkResponse;
    } catch (error) {
        console.log('Network failed, trying cache for:', request.url);
        
        // 网络失败，尝试从缓存获取
        const cachedResponse = await cache.match(request);
        
        if (cachedResponse) {
            // 检查缓存是否过期（5分钟）
            const cacheTimestamp = cachedResponse.headers.get('sw-cache-timestamp');
            if (cacheTimestamp && Date.now() - parseInt(cacheTimestamp) < 300000) {
                return cachedResponse;
            } else {
                // 缓存过期，删除
                cache.delete(request);
            }
        }
        
        // 返回离线响应
        return new Response(JSON.stringify({
            error: 'Network unavailable',
            message: '网络连接不可用，请检查网络设置'
        }), {
            status: 503,
            headers: { 'Content-Type': 'application/json' }
        });
    }
}

/**
 * 处理页面请求 - 缓存优先策略
 */
async function handlePageRequest(request) {
    try {
        // 先从缓存查找
        const cache = await caches.open(STATIC_CACHE_NAME);
        const cachedResponse = await cache.match(request);
        
        if (cachedResponse) {
            // 后台更新缓存
            updateCacheInBackground(request, cache);
            return cachedResponse;
        }

        // 缓存中没有，从网络获取
        const networkResponse = await fetch(request);
        
        if (networkResponse.ok) {
            cache.put(request, networkResponse.clone());
        }
        
        return networkResponse;
    } catch (error) {
        console.error('Page fetch failed:', error);
        
        // 返回缓存的首页或离线页面
        const cache = await caches.open(STATIC_CACHE_NAME);
        const fallbackResponse = await cache.match('/') || await cache.match('/offline.html');
        
        if (fallbackResponse) {
            return fallbackResponse;
        }
        
        // 返回简单的离线页面
        return new Response(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>离线模式 - 七牛云3D Generator</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                    .offline-message { max-width: 400px; margin: 0 auto; }
                    .retry-btn { 
                        background: #007bff; color: white; border: none; 
                        padding: 10px 20px; border-radius: 5px; cursor: pointer; 
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="offline-message">
                    <h1>🔌 离线模式</h1>
                    <p>当前网络连接不可用，请检查网络设置后重试。</p>
                    <button class="retry-btn" onclick="window.location.reload()">重新加载</button>
                </div>
            </body>
            </html>
        `, {
            headers: { 'Content-Type': 'text/html' }
        });
    }
}

/**
 * 后台更新缓存
 */
async function updateCacheInBackground(request, cache) {
    try {
        const networkResponse = await fetch(request);
        if (networkResponse.ok) {
            cache.put(request, networkResponse.clone());
        }
    } catch (error) {
        console.log('Background cache update failed:', error);
    }
}

/**
 * 处理消息事件
 */
self.addEventListener('message', (event) => {
    if (event.data && event.data.type) {
        switch (event.data.type) {
            case 'SKIP_WAITING':
                self.skipWaiting();
                break;
            case 'CLEAR_CACHE':
                clearAllCaches();
                break;
            case 'UPDATE_CACHE':
                updateStaticCache();
                break;
        }
    }
});

/**
 * 清理所有缓存
 */
async function clearAllCaches() {
    const cacheNames = await caches.keys();
    await Promise.all(cacheNames.map(name => caches.delete(name)));
    console.log('All caches cleared');
}

/**
 * 更新静态缓存
 */
async function updateStaticCache() {
    const cache = await caches.open(STATIC_CACHE_NAME);
    await cache.addAll(STATIC_ASSETS);
    console.log('Static cache updated');
}

/**
 * 定期清理过期缓存
 */
setInterval(async () => {
    const cache = await caches.open(API_CACHE_NAME);
    const requests = await cache.keys();
    
    for (const request of requests) {
        const response = await cache.match(request);
        const cacheTimestamp = response.headers.get('sw-cache-timestamp');
        
        if (cacheTimestamp && Date.now() - parseInt(cacheTimestamp) > 300000) {
            await cache.delete(request);
        }
    }
}, 600000); // 每10分钟清理一次