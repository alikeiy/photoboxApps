import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

/**
 * TurboModule spec for Canon EOS M10 / USB PTP control.
 * Implementation: android/.../CanonUsbModule.kt
 */
export interface Spec extends TurboModule {
  /** Check USB Host support and prepare UsbManager. */
  initializeUsb(): Promise<boolean>;

  /** Request runtime USB permission for the connected Canon device. */
  requestCameraPermission(): Promise<boolean>;

  /** Fire remote shutter via PTP (not implemented yet). */
  triggerShutter(): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('CanonUsb');
