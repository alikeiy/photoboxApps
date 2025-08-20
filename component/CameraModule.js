// src/modules/CameraModule.js
import {NativeModules} from 'react-native';

const {CameraModule: NativeCameraModule} = NativeModules;

export default {
  /**
   * Request camera permission via native module
   * @returns {Promise<'authorized' | 'denied'>}
   */
  requestCameraPermission: async () => {
    try {
      const result = await NativeCameraModule.requestCameraPermission();
      // Kotlin module kita sudah resolve dengan "authorized" atau "denied"
      return result;
    } catch (e) {
      console.error('CameraModule error:', e);
      return 'denied';
    }
  },
};
