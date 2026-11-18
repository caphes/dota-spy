import { deflateSync } from 'node:zlib';

function chunk(type, bytes) {
  const payload = Buffer.concat([Buffer.from(type), bytes]);
  const size = Buffer.alloc(4);
  size.writeUInt32BE(bytes.length);
  let crc = 0xffffffff;
  for (const byte of payload) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = crc & 1 ? (crc >>> 1) ^ 0xedb88320 : crc >>> 1;
  }
  const checksum = Buffer.alloc(4);
  checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0);
  return Buffer.concat([size, payload, checksum]);
}

const layers = [
  { points: [[64,14],[106,38],[106,90],[64,114],[22,90],[22,38]], color: [119,203,163] },
  { points: [[64,17],[103,40],[103,88],[64,111],[25,88],[25,40]], color: [34,66,54] },
  { points: [[84,36],[77,65],[61,81],[47,67],[63,51]], color: [186,247,214] },
  { points: [[84,36],[55,75],[77,65]], color: [74,156,117] },
  { points: [[47,62],[66,81],[61,86],[53,78],[43,88],[39,84],[49,74],[42,67]], color: [224,255,236] },
  { points: [[35,85],[43,93],[38,98],[30,90]], color: [224,255,236] }
];

function inside(points, x, y) {
  let filled = false;
  for (let index = 0; index < points.length; index++) {
    const [ax, ay] = points[index];
    const [bx, by] = points[(index + 1) % points.length];
    if ((ay > y) !== (by > y) && x < ax + (bx - ax) * (y - ay) / (by - ay)) filled = !filled;
  }
  return filled;
}

export function iconPng(size) {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(size, 0);
  header.writeUInt32BE(size, 4);
  header[8] = 8;
  header[9] = 2;
  const stride = size * 3 + 1;
  const pixels = Buffer.alloc(size * stride);
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const px = (x + 0.5) * 128 / size;
      const py = (y + 0.5) * 128 / size;
      const light = Math.max(0, 1 - Math.hypot(px - 64, py - 64) / 91);
      let color = [11 + 15 * light, 18 + 29 * light, 16 + 21 * light];
      for (const layer of layers) if (inside(layer.points, px, py)) color = layer.color;
      pixels.set(color, y * stride + 1 + x * 3);
    }
  }
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(pixels)),
    chunk('IEND', Buffer.alloc(0))
  ]);
}
