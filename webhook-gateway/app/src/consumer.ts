import Redis from 'ioredis';
import { ConsumerConfig, PaymentEvent } from './types';
import { generateSignature } from './signer';

export class StreamConsumer {
  private redis: Redis;
  private config: ConsumerConfig;
  private running = false;

  constructor(config: ConsumerConfig, redisClient?: Redis) {
    this.config = config;
    this.redis = redisClient || new Redis(config.redisUrl);
  }

  async initGroup(): Promise<void> {
    try {
      await this.redis.xgroup('CREATE', this.config.streamKey, this.config.groupName, '$', 'MKSTREAM');
    } catch (err: any) {
      if (!err.message?.includes('BUSYGROUP')) {
        throw err;
      }
    }
  }

  async processMessage(id: string, fields: Record<string, string>): Promise<{ signature: string; event?: PaymentEvent }> {
    const rawData = fields.payload || '{}';
    const signature = generateSignature(rawData, this.config.webhookSecret);
    let event: PaymentEvent | undefined;
    try {
      event = JSON.parse(rawData) as PaymentEvent;
      console.log(`[WebhookGateway] Processed event ID: ${id} | Order: ${event.orderId || 'N/A'} | Status: ${event.status || 'PROCESSED'} | Signature: ${signature.substring(0, 8)}...`);
    } catch {
      console.log(`[WebhookGateway] Processed event ID: ${id} | Signature: ${signature.substring(0, 8)}...`);
    }
    await this.redis.xack(this.config.streamKey, this.config.groupName, id);
    return { signature, event };
  }

  async start(): Promise<void> {
    await this.initGroup();
    this.running = true;
    console.log(`[WebhookGateway] Consumer started on stream: ${this.config.streamKey}`);

    while (this.running) {
      try {
        const response: any = await this.redis.xreadgroup(
          'GROUP', this.config.groupName, this.config.consumerName,
          'COUNT', 10,
          'BLOCK', 2000,
          'STREAMS', this.config.streamKey, '>'
        );

        if (response && Array.isArray(response) && response.length > 0) {
          const streamData = response[0];
          const messages = streamData && streamData[1];
          if (Array.isArray(messages)) {
            for (const [id, fieldArray] of messages) {
              const fields: Record<string, string> = {};
              if (Array.isArray(fieldArray)) {
                for (let i = 0; i < fieldArray.length; i += 2) {
                  fields[fieldArray[i]] = fieldArray[i + 1];
                }
              }
              await this.processMessage(id, fields);
            }
          }
        }
      } catch (err) {
        if (!this.running) break;
        console.error('[WebhookGateway] Error reading stream:', err);
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
    }
  }

  async stop(): Promise<void> {
    this.running = false;
    try {
      await this.redis.quit();
    } catch {
      this.redis.disconnect();
    }
  }
}
