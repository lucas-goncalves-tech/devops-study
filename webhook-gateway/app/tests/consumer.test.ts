import { describe, it, expect, vi } from 'vitest';
import { StreamConsumer } from '../src/consumer';

describe('StreamConsumer', () => {
  it('should create consumer group successfully', async () => {
    const mockRedis = {
      xgroup: vi.fn().mockResolvedValue('OK'),
      xack: vi.fn().mockResolvedValue(1),
      quit: vi.fn().mockResolvedValue('OK'),
    } as any;

    const consumer = new StreamConsumer(
      {
        redisUrl: 'redis://localhost:6379',
        streamKey: 'test-stream',
        groupName: 'test-group',
        consumerName: 'test-consumer',
        webhookSecret: 'secret123',
      },
      mockRedis
    );

    await consumer.initGroup();
    expect(mockRedis.xgroup).toHaveBeenCalledWith(
      'CREATE',
      'test-stream',
      'test-group',
      '$',
      'MKSTREAM'
    );
  });

  it('should ignore BUSYGROUP error gracefully', async () => {
    const mockRedis = {
      xgroup: vi.fn().mockRejectedValue(new Error('BUSYGROUP Consumer Group name already exists')),
      xack: vi.fn().mockResolvedValue(1),
      quit: vi.fn().mockResolvedValue('OK'),
    } as any;

    const consumer = new StreamConsumer(
      {
        redisUrl: 'redis://localhost:6379',
        streamKey: 'test-stream',
        groupName: 'test-group',
        consumerName: 'test-consumer',
        webhookSecret: 'secret123',
      },
      mockRedis
    );

    await expect(consumer.initGroup()).resolves.not.toThrow();
  });

  it('should process message, generate HMAC signature, and ack message', async () => {
    const mockRedis = {
      xack: vi.fn().mockResolvedValue(1),
      quit: vi.fn().mockResolvedValue('OK'),
    } as any;

    const consumer = new StreamConsumer(
      {
        redisUrl: 'redis://localhost:6379',
        streamKey: 'test-stream',
        groupName: 'test-group',
        consumerName: 'test-consumer',
        webhookSecret: 'test-secret',
      },
      mockRedis
    );

    const payload = JSON.stringify({ eventId: '123', orderId: 'order-999', amount: 100, currency: 'BRL', status: 'PROCESSED', timestamp: '2026-09-09T00:00:00Z' });
    const result = await consumer.processMessage('msg-1', { payload });

    expect(result.signature).toHaveLength(64);
    expect(result.event?.orderId).toBe('order-999');
    expect(mockRedis.xack).toHaveBeenCalledWith('test-stream', 'test-group', 'msg-1');
  });

  it('should process malformed JSON payload without crashing', async () => {
    const mockRedis = {
      xack: vi.fn().mockResolvedValue(1),
      quit: vi.fn().mockResolvedValue('OK'),
    } as any;

    const consumer = new StreamConsumer(
      {
        redisUrl: 'redis://localhost:6379',
        streamKey: 'test-stream',
        groupName: 'test-group',
        consumerName: 'test-consumer',
        webhookSecret: 'test-secret',
      },
      mockRedis
    );

    const result = await consumer.processMessage('msg-2', { payload: 'not-a-json' });

    expect(result.signature).toHaveLength(64);
    expect(result.event).toBeUndefined();
    expect(mockRedis.xack).toHaveBeenCalledWith('test-stream', 'test-group', 'msg-2');
  });

  it('should fallback to disconnect if quit fails during stop', async () => {
    const mockRedis = {
      quit: vi.fn().mockRejectedValue(new Error('Connection lost')),
      disconnect: vi.fn(),
    } as any;

    const consumer = new StreamConsumer(
      {
        redisUrl: 'redis://localhost:6379',
        streamKey: 'test-stream',
        groupName: 'test-group',
        consumerName: 'test-consumer',
        webhookSecret: 'test-secret',
      },
      mockRedis
    );

    await consumer.stop();
    expect(mockRedis.disconnect).toHaveBeenCalled();
  });
});
