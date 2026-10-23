const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function loginAdmin() {
  const resp = await fetch('http://localhost:8080/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'admin@shopsphere.com',
      password: 'Admin@12345'
    })
  });
  if (!resp.ok) throw new Error('Login failed: ' + resp.status);
  return await resp.json();
}

async function run() {
  const auth = await loginAdmin();
  const token = auth.accessToken;
  const user = {
    userId: auth.userId,
    email: auth.email,
    firstName: auth.firstName || 'ShopSphere',
    lastName: auth.lastName || 'Admin',
    roles: auth.roles || ['CUSTOMER', 'ADMIN']
  };

  const userDataDir = path.join(__dirname, 'chrome_cdp_profile');
  const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

  const chrome = spawn(chromePath, [
    '--remote-debugging-port=9222',
    '--headless=new',
    '--disable-gpu',
    `--user-data-dir=${userDataDir}`,
    '--window-size=1400,1000',
    'http://localhost:5173/'
  ]);

  await sleep(3000);

  try {
    const listResp = await fetch('http://127.0.0.1:9222/json/list');
    const tabs = await listResp.json();
    const pageTab = tabs.find(t => t.type === 'page');
    if (!pageTab) throw new Error('No page tab found');

    const ws = new WebSocket(pageTab.webSocketDebuggerUrl);
    let msgId = 1;
    const pending = new Map();

    ws.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      if (msg.id && pending.has(msg.id)) {
        pending.get(msg.id)(msg);
        pending.delete(msg.id);
      }
    };

    function send(method, params = {}) {
      return new Promise((resolve) => {
        const id = msgId++;
        pending.set(id, resolve);
        ws.send(JSON.stringify({ id, method, params }));
      });
    }

    await new Promise(r => ws.onopen = r);
    await send('Page.enable');
    await send('Runtime.enable');

    // 1. Set localStorage tokens
    await send('Runtime.evaluate', {
      expression: `
        localStorage.setItem('shopsphere_access_token', ${JSON.stringify(token)});
        localStorage.setItem('shopsphere_user_info', ${JSON.stringify(JSON.stringify(user))});
      `
    });

    // 2. Navigate to /admin/products and wait for table
    console.log('Navigating to /admin/products...');
    await send('Page.navigate', { url: 'http://localhost:5173/admin/products' });
    await sleep(6000);

    const shot1 = await send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(path.join(__dirname, 'admin_products.png'), Buffer.from(shot1.result.data, 'base64'));
    console.log('Saved admin_products.png');

    // 3. Navigate to /admin (Dashboard)
    console.log('Navigating to /admin...');
    await send('Page.navigate', { url: 'http://localhost:5173/admin' });
    await sleep(6000);

    const shot2 = await send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(path.join(__dirname, 'admin_dashboard.png'), Buffer.from(shot2.result.data, 'base64'));
    console.log('Saved admin_dashboard.png');

    ws.close();
  } finally {
    chrome.kill();
  }
}

run().catch(console.error);
