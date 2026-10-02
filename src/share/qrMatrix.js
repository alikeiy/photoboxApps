import qrcode from 'qrcode-generator';

/** "count:0101..." module bits, row-major, for the Android canvas QR slot. */
export function encodeQrMatrix(value) {
  const qr = qrcode(0, 'M');
  qr.addData(value);
  qr.make();
  const count = qr.getModuleCount();
  let bits = '';
  for (let row = 0; row < count; row += 1) {
    for (let col = 0; col < count; col += 1) {
      bits += qr.isDark(row, col) ? '1' : '0';
    }
  }
  return `${count}:${bits}`;
}

export function readQrMatrix(value) {
  const encoded = encodeQrMatrix(value);
  const split = encoded.indexOf(':');
  const count = Number(encoded.slice(0, split));
  const bits = encoded.slice(split + 1);
  return {count, bits};
}
