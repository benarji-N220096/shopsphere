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

  const userDataDir = path.join(__dirname, 'chrome_cart_auth_profile');
  const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

  const chrome = spawn(chromePath, [
    '--remote-debugging-port=9224',
    '--headless=new',
    '--disable-gpu',
    `--user-data-dir=${userDataDir}`,
    '--window-size=1280,1000',
    'http://localhost:5173/'
  ]);

  await sleep(3000);

  try {
    const listResp = await fetch('http://127.0.0.1:9224/json/list');
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

    const sampleCart = [
      {
        productId: '1b5d9458-b2c5-4b33-b8f4-6b488d28bba5',
        quantity: 1,
        product: {
          id: '1b5d9458-b2c5-4b33-b8f4-6b488d28bba5',
          name: 'Ultra HD 4K Gaming Monitor (27-inch, 144Hz)',
          price: 28999.00,
          category: 'Monitors',
          imageUrl: '/images/products/monitor.jpg',
          stockQuantity: 25
        }
      }
    ];

    await send('Runtime.evaluate', {
      expression: `
        localStorage.setItem('shopsphere_access_token', ${JSON.stringify(token)});
        localStorage.setItem('shopsphere_user_info', ${JSON.stringify(JSON.stringify(user))});
        localStorage.setItem('shopsphere_guest_cart', ${JSON.stringify(JSON.stringify(sampleCart))});
      `
    });

    console.log('Navigating to /checkout...');
    await send('Page.navigate', { url: 'http://localhost:5173/checkout' });
    await sleep(4000);

    const shot = await send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(path.join(__dirname, 'checkout_page.png'), Buffer.from(shot.result.data, 'base64'));
    console.log('Saved authenticated checkout_page.png');

    ws.close();
  } finally {
    chrome.kill();
  }
}

run().catch(console.error);
