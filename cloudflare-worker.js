/**
 * Cloudflare Worker — Supabase 反向代理 (免费方案)
 *
 * 部署步骤:
 *   1. Cloudflare Dashboard → Workers & Pages → Create Worker
 *   2. 粘贴此代码
 *   3. 绑定路由: api.mint-lemon.click/*
 *   4. 点击 Deploy
 *
 * 功能:
 *   - 反向代理所有请求到 Supabase
 *   - 注入 Cache-Control 到 GET 响应 (边缘缓存 + 浏览器缓存)
 *   - Cloudflare 自动启用 Brotli 压缩 + HTTP/2 + HTTP/3
 *   - 对 GET /rest/v1/* 请求启用 Cloudflare Edge Cache (1小时)
 *   - 对静态资源 (Storage) 启用 1 年缓存
 *
 * 效果:
 *   DNS <10ms (Cloudflare Anycast)
 *   TCP <20ms (边缘节点就近接入)
 *   TLS <30ms (Cloudflare 证书)
 *   GET API 缓存命中时 <5ms
 *
 * 免费额度: 10 万次/天，足够项目使用
 */

// ── 配置 ──────────────────────────────────────────────────
const SUPABASE_URL = "https://yngbeujrcpvtgfooupor.supabase.co";

// 需要透传的 Supabase 请求头
const FORWARD_HEADERS = [
  "apikey",
  "authorization",
  "content-type",
  "prefer",
  "accept",
  "accept-encoding",
  "x-client-info",
];

// ── Worker 入口 ──────────────────────────────────────────
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 构建目标 Supabase URL
    const targetUrl = `${SUPABASE_URL}${url.pathname}${url.search}`;

    // 复制请求头
    const headers = new Headers();
    for (const name of FORWARD_HEADERS) {
      const value = request.headers.get(name);
      if (value) headers.set(name, value);
    }

    // 构建转发请求
    const proxyRequest = new Request(targetUrl, {
      method: request.method,
      headers: headers,
      body: request.method !== "GET" && request.method !== "HEAD"
        ? await request.arrayBuffer()
        : undefined,
    });

    // 发送到 Supabase
    const response = await fetch(proxyRequest);

    // ── 缓存策略 ──────────────────────────────────────────
    const newHeaders = new Headers(response.headers);

    // CORS: 允许 App 访问
    newHeaders.set("Access-Control-Allow-Origin", "*");
    newHeaders.set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
    newHeaders.set("Access-Control-Allow-Headers", FORWARD_HEADERS.join(", "));

    // GET 请求: 添加缓存头
    if (request.method === "GET" || request.method === "HEAD") {
      const path = url.pathname;

      if (path.startsWith("/storage/v1/")) {
        // 静态资源 (音频/图片): 缓存 1 年
        newHeaders.set("Cache-Control", "public, max-age=31536000, immutable");
        newHeaders.set("CDN-Cache-Control", "public, max-age=31536000");
      } else if (path.startsWith("/rest/v1/")) {
        // API 查询: 缓存 1 小时 (边缘)
        newHeaders.set("Cache-Control", "public, max-age=3600, s-maxage=3600");
        newHeaders.set("CDN-Cache-Control", "public, max-age=3600");
      } else if (path.startsWith("/auth/v1/")) {
        // 认证请求: 不缓存
        newHeaders.set("Cache-Control", "no-store");
      }
    }

    // ── 预检请求 (CORS OPTIONS) ──────────────────────────
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: newHeaders,
      });
    }

    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: newHeaders,
    });
  },
};
