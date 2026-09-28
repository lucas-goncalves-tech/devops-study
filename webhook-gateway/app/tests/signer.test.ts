import { describe, it, expect } from 'vitest';
import { generateSignature, verifySignature } from '../src/signer';

describe('HMAC Signer', () => {
  it('should generate consistent HMAC-SHA256 signature for payload', () => {
    const payload = JSON.stringify({ event: 'PAYMENT_PROCESSED', id: 'tx-123', amount: 5000 });
    const secret = 'super-secret-key';
    const sig1 = generateSignature(payload, secret);
    const sig2 = generateSignature(payload, secret);

    expect(sig1).toHaveLength(64);
    expect(sig1).toBe(sig2);
    expect(verifySignature(payload, secret, sig1)).toBe(true);
    expect(verifySignature(payload, 'wrong-secret', sig1)).toBe(false);
    expect(verifySignature(payload, secret, 'invalidlength')).toBe(false);
  });

  it('should return false safely without throwing on non-hex string of length 64', () => {
    const payload = 'test-payload';
    const secret = 'secret';
    // String with length 64 containing non-hex chars ('g')
    const nonHexSig = 'g'.repeat(64);
    expect(verifySignature(payload, secret, nonHexSig)).toBe(false);
  });

  it('should return false on tampered signature of correct length', () => {
    const payload = 'test-payload';
    const secret = 'secret';
    const validSig = generateSignature(payload, secret);
    // Tamper with last character
    const tampered = validSig.substring(0, 63) + (validSig[63] === '0' ? '1' : '0');
    expect(verifySignature(payload, secret, tampered)).toBe(false);
  });

  it('should reject non-string inputs safely', () => {
    expect(verifySignature(null as any, 'secret', 'sig')).toBe(false);
    expect(verifySignature('payload', null as any, 'sig')).toBe(false);
    expect(verifySignature('payload', 'secret', null as any)).toBe(false);
  });
});
