import {TurboModuleRegistry} from 'react-native';
import type {Spec} from '../specs/NativePhotoBooth';
import {frameById} from '../config/frames';

const NativePhotoBooth = TurboModuleRegistry.get<Spec>('PhotoBooth');

function unavailable(method: string): Promise<never> {
  return Promise.reject(
    new Error(`PhotoBooth tidak tersedia (${method}). Rebuild aplikasi Android.`),
  );
}

export type BoothFrame = {id: string; name: string};

export const PhotoBooth = {
  isAvailable: (): boolean => NativePhotoBooth != null,

  consumePendingImport: (): Promise<string> =>
    NativePhotoBooth?.consumePendingImport() ??
    unavailable('consumePendingImport'),

  latestImage: (): Promise<string> =>
    NativePhotoBooth?.latestImage() ?? unavailable('latestImage'),

  listFrames: async (): Promise<BoothFrame[]> => {
    const raw = await (NativePhotoBooth?.listFrames() ?? unavailable('listFrames'));
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.map(frame => {
      const known = frameById(frame.id);
      return known ? {...frame, name: known.name} : frame;
    });
  },

  compose: (
    photoUri: string,
    frameId: string,
    maxEdge: number,
    layoutJson: string = '',
    qrMatrix: string = '',
  ): Promise<string> =>
    NativePhotoBooth?.compose(photoUri, frameId, maxEdge, layoutJson, qrMatrix) ??
    unavailable('compose'),
};

export default PhotoBooth;
