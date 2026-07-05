import NativeCanonUsb from '../specs/NativeCanonUsb';

export const CanonUsb = {
  initializeUsb: (): Promise<boolean> => NativeCanonUsb.initializeUsb(),

  requestCameraPermission: (): Promise<boolean> =>
    NativeCanonUsb.requestCameraPermission(),

  triggerShutter: (): Promise<boolean> => NativeCanonUsb.triggerShutter(),
};

export default CanonUsb;
