import {TurboModuleRegistry} from 'react-native';
import type {Spec} from '../specs/NativeShutterTrigger';

const NativeShutterTrigger = TurboModuleRegistry.get<Spec>('ShutterTrigger');

function unavailable(method: string): Promise<never> {
  return Promise.reject(
    new Error(
      `ShutterTrigger tidak tersedia (${method}). Rebuild aplikasi Android.`,
    ),
  );
}

export const ShutterTrigger = {
  isAvailable: (): boolean => NativeShutterTrigger != null,

  canDrawOverlays: (): Promise<boolean> =>
    NativeShutterTrigger?.canDrawOverlays() ?? unavailable('canDrawOverlays'),

  openOverlaySettings: (): Promise<boolean> =>
    NativeShutterTrigger?.openOverlaySettings() ??
    unavailable('openOverlaySettings'),

  isAccessibilityEnabled: (): Promise<boolean> =>
    NativeShutterTrigger?.isAccessibilityEnabled() ??
    unavailable('isAccessibilityEnabled'),

  openAccessibilitySettings: (): Promise<boolean> =>
    NativeShutterTrigger?.openAccessibilitySettings() ??
    unavailable('openAccessibilitySettings'),

  isIgnoringBatteryOptimizations: (): Promise<boolean> =>
    NativeShutterTrigger?.isIgnoringBatteryOptimizations() ??
    unavailable('isIgnoringBatteryOptimizations'),

  requestIgnoreBatteryOptimizations: (): Promise<boolean> =>
    NativeShutterTrigger?.requestIgnoreBatteryOptimizations() ??
    unavailable('requestIgnoreBatteryOptimizations'),

  openOemAutostartSettings: (): Promise<boolean> =>
    NativeShutterTrigger?.openOemAutostartSettings() ??
    unavailable('openOemAutostartSettings'),

  startTrigger: (): Promise<boolean> =>
    NativeShutterTrigger?.startTrigger() ?? unavailable('startTrigger'),

  stopTrigger: (): Promise<boolean> =>
    NativeShutterTrigger?.stopTrigger() ?? unavailable('stopTrigger'),

  isTriggerRunning: (): Promise<boolean> =>
    NativeShutterTrigger?.isTriggerRunning() ?? unavailable('isTriggerRunning'),

  setPinLocked: (locked: boolean): Promise<boolean> =>
    NativeShutterTrigger?.setPinLocked(locked) ?? unavailable('setPinLocked'),

  isPinLocked: (): Promise<boolean> =>
    NativeShutterTrigger?.isPinLocked() ?? unavailable('isPinLocked'),

  isAnchorValid: (): Promise<boolean> =>
    NativeShutterTrigger?.isAnchorValid() ?? unavailable('isAnchorValid'),

  isDisplayLandscape: (): Promise<boolean> =>
    NativeShutterTrigger?.isDisplayLandscape() ??
    unavailable('isDisplayLandscape'),

  getAnchorX: (): Promise<number> =>
    NativeShutterTrigger?.getAnchorX() ?? unavailable('getAnchorX'),

  getAnchorY: (): Promise<number> =>
    NativeShutterTrigger?.getAnchorY() ?? unavailable('getAnchorY'),

  fireTestTap: (): Promise<boolean> =>
    NativeShutterTrigger?.fireTestTap() ?? unavailable('fireTestTap'),
};

export default ShutterTrigger;
