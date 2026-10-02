import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

/**
 * Shutter pin that stays above Canon Camera Connect and taps a saved point
 * when a volume key or Bluetooth remote is pressed.
 * Implementation: android/.../trigger/ShutterTriggerModule.kt
 */
export interface Spec extends TurboModule {
  canDrawOverlays(): Promise<boolean>;
  openOverlaySettings(): Promise<boolean>;
  isAccessibilityEnabled(): Promise<boolean>;
  openAccessibilitySettings(): Promise<boolean>;
  isIgnoringBatteryOptimizations(): Promise<boolean>;
  requestIgnoreBatteryOptimizations(): Promise<boolean>;
  /** Opens Realme / ColorOS autostart list when that screen exists. */
  openOemAutostartSettings(): Promise<boolean>;
  startTrigger(): Promise<boolean>;
  stopTrigger(): Promise<boolean>;
  isTriggerRunning(): Promise<boolean>;
  setPinLocked(locked: boolean): Promise<boolean>;
  isPinLocked(): Promise<boolean>;
  isAnchorValid(): Promise<boolean>;
  isDisplayLandscape(): Promise<boolean>;
  getAnchorX(): Promise<number>;
  getAnchorY(): Promise<number>;
  fireTestTap(): Promise<boolean>;
}

export default TurboModuleRegistry.get<Spec>('ShutterTrigger');
