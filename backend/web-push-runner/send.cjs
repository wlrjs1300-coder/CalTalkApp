'use strict';

const webpush = require('web-push');

let input = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (chunk) => { input += chunk; });
process.stdin.on('end', async () => {
  try {
    const request = JSON.parse(input);
    webpush.setVapidDetails(
      process.env.WEB_PUSH_VAPID_SUBJECT,
      process.env.WEB_PUSH_VAPID_PUBLIC_KEY,
      process.env.WEB_PUSH_VAPID_PRIVATE_KEY,
    );
    const response = await webpush.sendNotification(
      {
        endpoint: request.endpoint,
        keys: { p256dh: request.p256dh, auth: request.auth },
      },
      request.payload,
      { TTL: 60, urgency: 'high' },
    );
    process.stdout.write(JSON.stringify({ status: response.statusCode }));
  } catch (error) {
    const status = error.statusCode || 'ERROR';
    const detail = error.body || error.message || 'Unknown web push error';
    process.stderr.write(`Web Push ${status}: ${detail}`);
    process.exitCode = 1;
  }
});
