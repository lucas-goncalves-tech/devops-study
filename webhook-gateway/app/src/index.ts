import dotenv from 'dotenv';
import { StreamConsumer } from './consumer';

dotenv.config();

const config = {
  redisUrl: process.env.REDIS_URL || 'redis://localhost:6379',
  streamKey: process.env.STREAM_KEY || 'payment-events',
  groupName: process.env.GROUP_NAME || 'webhook-dispatcher-group',
  consumerName: process.env.CONSUMER_NAME || `worker-${process.pid}`,
  webhookSecret: process.env.WEBHOOK_SECRET || 'default-webhook-secret-key-32chars'
};

const consumer = new StreamConsumer(config);

consumer.start().catch((err) => {
  console.error('[WebhookGateway] Fatal error:', err);
  process.exit(1);
});

const shutdown = async (signal: string) => {
  console.log(`[WebhookGateway] Received ${signal}, shutting down gracefully...`);
  await consumer.stop();
  process.exit(0);
};

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
