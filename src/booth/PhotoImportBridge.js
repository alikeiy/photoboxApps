import {useEffect, useRef} from 'react';
import {AppState, DeviceEventEmitter} from 'react-native';
import PhotoBooth from '../native/PhotoBooth';

/**
 * Opens frame selection when Canon Camera Connect has finished saving a photo.
 * The native watcher also keeps the URI until this bridge consumes it, so a
 * resume after the activity was in the background still lands on the frame screen.
 */
export default function PhotoImportBridge({navigationRef}) {
  const lastNavAt = useRef(0);

  useEffect(() => {
    let alive = true;

    const open = uri => {
      if (!alive || !uri) return;
      if (!navigationRef.isReady()) {
        setTimeout(() => open(uri), 250);
        return;
      }
      const now = Date.now();
      if (now - lastNavAt.current < 1500) return;
      lastNavAt.current = now;
      navigationRef.navigate('FrameSelect', {photoUri: uri});
    };

    const pull = async () => {
      if (!alive || !PhotoBooth.isAvailable()) return;
      if (!navigationRef.isReady()) {
        setTimeout(pull, 250);
        return;
      }
      try {
        const uri = await PhotoBooth.consumePendingImport();
        if (alive && uri) open(uri);
      } catch (error) {
        console.warn('consumePendingImport', error);
      }
    };

    pull();
    const appState = AppState.addEventListener('change', next => {
      if (next === 'active') pull();
    });
    const imported = DeviceEventEmitter.addListener(
      'photoboxPhotoImported',
      uri => {
        if (typeof uri === 'string' && uri) {
          PhotoBooth.consumePendingImport().catch(() => {});
          open(uri);
        } else {
          pull();
        }
      },
    );

    return () => {
      alive = false;
      appState.remove();
      imported.remove();
    };
  }, [navigationRef]);

  return null;
}
