// ============================================================
// 场景一：并发正确性 —— 75 个工程师同时抢同一张单
//
// 目的：验证「一单只能被一人抢到」，看有没有超抢。
// 断言：抢单成功数必须恰好为 1。
//
// 用法：
//   k6 run k6/grab-race.js
//   k6 run -e ORDER_ID=19 k6/grab-race.js      # 换一张单再跑
// ============================================================

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS || '75', 10);
const ORDER_ID = __ENV.ORDER_ID || '18';

const grabSuccess = new Counter('grab_success');
const grabLost = new Counter('grab_lost');
const grabOther = new Counter('grab_other');

export const options = {
    // 75 个账号要登录，BCrypt cost=12 单次约 300ms，给足时间
    setupTimeout: '180s',
    batch: VUS,
    batchPerHost: VUS,
    scenarios: {
        race: {
            executor: 'per-vu-iterations',
            vus: VUS,
            iterations: 1,
            maxDuration: '60s',
        },
    },
};

// 每个 VU 一个账号，先在 setup 里并发登录拿 token
export function setup() {
    const reqs = [];
    for (let i = 1; i <= VUS; i++) {
        const id = String(i).padStart(2, '0');
        reqs.push({
            method: 'POST',
            url: `${BASE}/user/login/email`,
            body: JSON.stringify({ email: `perf${id}@test.com`, password: 'Test@1234' }),
            params: { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
        });
    }
    const resps = http.batch(reqs);
    const tokens = [];
    resps.forEach((r) => {
        if (r.status === 200) {
            try {
                tokens.push(JSON.parse(r.body).data.saTokenInfo.tokenValue);
            } catch (e) {
                // 忽略解析失败的
            }
        }
    });
    console.log(`[setup] 登录成功 ${tokens.length}/${VUS}`);
    return { tokens };
}

export default function (data) {
    const token = data.tokens[__VU - 1];
    if (!token) {
        grabOther.add(1);
        return;
    }

    const res = http.post(`${BASE}/order/${ORDER_ID}/grab`, null, {
        headers: { satoken: token },
        tags: { name: 'grab' },
    });
    const body = res.body || '';

    if (body.indexOf('抢单成功') >= 0) {
        grabSuccess.add(1);
        check(res, { '抢到了': () => true });
    } else if (body.indexOf('手慢了') >= 0 || body.indexOf('已经抢到') >= 0) {
        grabLost.add(1);
    } else {
        grabOther.add(1);
        console.log(`[异常响应] ${body.slice(0, 150)}`);
    }
}

export function handleSummary(data) {
    const pick = (name) => (data.metrics[name] ? data.metrics[name].values.count : 0);
    const success = pick('grab_success');
    const lost = pick('grab_lost');
    const other = pick('grab_other');
    const pass = success === 1 && lost === VUS - 1;

    const line = '==================================================';
    const summary = [
        '',
        line,
        `  工单 #${ORDER_ID}   并发用户 ${VUS}`,
        line,
        `  抢单成功        ${success}`,
        `  手慢了 (409)    ${lost}`,
        `  异常响应        ${other}`,
        line,
        pass ? '  ✔ 通过：恰好 1 人抢到，没有超抢' : '  ✘ 失败：出现超抢或异常响应',
        line,
        '',
    ].join('\n');

    return {
        stdout: summary,
        'k6-race-summary.json': JSON.stringify(data, null, 2),
    };
}
