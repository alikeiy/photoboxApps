/**
 * Frame sheets in android/app/src/main/assets/frames/.
 * Slot numbers are pixels on the source PNG (top-left origin).
 * FrameComposer scales them onto the output canvas.
 *
 * frame_cute_pink.png is 2816×1536. The four windows and the QR hole
 * were measured from fully transparent pixels (alpha < 32).
 */
const frameCatalog = {
  frame_cute_pink: {
    id: 'frame_cute_pink',
    name: 'Cute & Pink',
    asset: 'frame_cute_pink.png',
    canvas: {width: 2816, height: 1536},
    photoSlots: [
      {x: 590, y: 306, w: 368, h: 492},
      {x: 1013, y: 310, w: 373, h: 482},
      {x: 1435, y: 306, w: 372, h: 488},
      {x: 1857, y: 314, w: 363, h: 485},
    ],
    qrSlot: {x: 1192, y: 886, w: 436, h: 425},
  },
};

export function layoutPayload(frameId) {
  const frame = frameCatalog[frameId];
  if (!frame?.photoSlots?.length) return '';
  return JSON.stringify({
    width: frame.canvas.width,
    height: frame.canvas.height,
    photoSlots: frame.photoSlots,
    qrSlot: frame.qrSlot ?? null,
  });
}

export function frameById(frameId) {
  return frameCatalog[frameId] ?? null;
}

export default frameCatalog;
