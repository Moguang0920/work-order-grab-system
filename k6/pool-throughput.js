// ============================================================
// 场景二：读吞吐 —— 持续压抢单池
//
// 目的：测「带鉴权的分页查询」能扛多少 QPS。
// 注意：每次请求都会触发 Sa-Token 鉴权 -> StpInterfaceImpl 查库（角色 + 权限 2 条 SQL），
//      所以这个数字反映的是「鉴权 + 分页查询」的组合性能。
//
// 用法：
//   k6 run k6/pool-throughput.js
//   k6 run -e VUS=100 -e DURATION=30s k6/pool-throughput.js
// ============================================================

import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS || '50', 10);
const DURATION = __ENV.DURATION || '20s';

export const options = {
    setupTimeout: '180s',
    batch: 75,
    batchPerHost: 75,
    scenarios: {
        pool: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    const reqs = [];
    for (let i = 1; i <= 75; i++) {
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
                // ignore
            }
        }
    });
    console.log(`[setup] 登录成功 ${tokens.length}/75`);
    return { tokens };
}

export default function (data) {
    const token = data.tokens[__VU % data.tokens.length];
    const res = http.get(`${BASE}/order/pool?page=1&size=10`, {
        headers: { satoken: token },
        tags: { name: 'pool' },
    });
    check(res, {
        '200 且业务成功': (r) => r.status === 200 && (r.body || '').indexOf('"statusCode":200') >= 0,
    });
}
