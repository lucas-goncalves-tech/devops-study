export interface PaymentEvent {
  eventId: string;
  orderId: string;
  amount: number;
  currency: string;
  status: 'PROCESSED' | 'FAILED';
  timestamp: string;
}

export interface ConsumerConfig {
  redisUrl: string;
  streamKey: string;
  groupName: string;
  consumerName: string;
  webhookSecret: string;
}
