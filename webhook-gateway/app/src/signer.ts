import crypto from 'crypto';

export function generateSignature(payload: string, secret: string): string {
  return crypto.createHmac('sha256', secret).update(payload).digest('hex');
}

export function verifySignature(payload: string, secret: string, signature: string): boolean {
  if (typeof payload !== 'string' || typeof secret !== 'string' || typeof signature !== 'string') {
    return false;
  }
  const expected = generateSignature(payload, secret);
  if (expected.length !== signature.length) {
    return false;
  }
  const expectedBuf = Buffer.from(expected, 'hex');
  const signatureBuf = Buffer.from(signature, 'hex');
  if (expectedBuf.length !== signatureBuf.length) {
    return false;
  }
  return crypto.timingSafeEqual(expectedBuf, signatureBuf);
}
