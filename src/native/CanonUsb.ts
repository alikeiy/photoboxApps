import {TurboModuleRegistry} from 'react-native';
import type {Spec} from '../specs/NativeCanonUsb';

const NativeCanonUsb = TurboModuleRegistry.get<Spec>('CanonUsb');

function unavailable(method: string): never {
  throw new Error(
    `CanonUsb native module tidak tersedia (${method}). Rebuild APK release/debug.`,
  );
}

export const CanonUsb = {
  initializeUsb: (): Promise<boolean> =>
    NativeCanonUsb?.initializeUsb() ?? unavailable('initializeUsb'),

  requestCameraPermission: (): Promise<boolean> =>
    NativeCanonUsb?.requestCameraPermission() ??
    unavailable('requestCameraPermission'),

  requestUvcPermission: (): Promise<boolean> =>
    NativeCanonUsb?.requestUvcPermission() ?? unavailable('requestUvcPermission'),

  triggerShutter: (): Promise<boolean> =>
    NativeCanonUsb?.triggerShutter() ?? unavailable('triggerShutter'),
};

export default CanonUsb;
