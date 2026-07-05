import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import CanonUsb from '../src/native/CanonUsb';
import ErrorBoundary from './ErrorBoundary';

let UvcCamera = null;
try {
  UvcCamera = require('react-native-uvc-camera').UvcCamera;
} catch (error) {
  console.warn('UvcCamera native module unavailable:', error);
}

const COUNTDOWN_SECONDS = 3;

/**
 * Classic photo booth screen:
 * - HDMI live preview via UVC capture card (UvcCamera)
 * - Shutter via USB PTP to Canon EOS M10 (CanonUsb)
 *
 * Hardware: see docs/HARDWARE.md
 */
export default function CameraScreen({navigation}) {
  const uvcRef = useRef(null);
  const [hdmiPreviewEnabled, setHdmiPreviewEnabled] = useState(false);
  const [hdmiReady, setHdmiReady] = useState(false);
  const [hdmiError, setHdmiError] = useState(null);
  const [canonReady, setCanonReady] = useState(false);
  const [status, setStatus] = useState('Menyiapkan photo booth…');
  const [countdown, setCountdown] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        await CanonUsb.initializeUsb();
        setStatus(
          'Tap "Nyalakan HDMI Preview" (capture card) atau "Hubungkan Canon" (shutter USB)',
        );
      } catch (error) {
        console.error('initializeUsb error:', error);
        setStatus('Gagal inisialisasi USB Host');
      }
    })();
  }, []);

  const onEnableHdmiPreview = useCallback(async () => {
    setBusy(true);
    try {
      const ok = await CanonUsb.requestUvcPermission();
      if (!ok) {
        setStatus(
          'Capture card tidak terdeteksi — colok HDMI capture card ke USB hub (bukan kabel Canon)',
        );
        return;
      }
      setHdmiError(null);
      setHdmiReady(false);
      setHdmiPreviewEnabled(true);
      setStatus('Menyalakan HDMI preview…');
    } catch (error) {
      console.error('requestUvcPermission error:', error);
      setStatus(`Gagal izin capture card: ${error.message ?? error}`);
    } finally {
      setBusy(false);
    }
  }, []);

  const onConnectCanon = useCallback(async () => {
    setBusy(true);
    try {
      const ok = await CanonUsb.requestCameraPermission();
      setCanonReady(ok);
      setStatus(
        ok
          ? 'Canon siap — HDMI preview di layar, tap Ambil Foto untuk shutter'
          : 'Canon gagal — cek kabel USB PTP & mode Photo transfer',
      );
    } catch (error) {
      console.error('requestCameraPermission error:', error);
      setStatus(`Gagal hubungkan Canon: ${error.message ?? error}`);
    } finally {
      setBusy(false);
    }
  }, []);

  const takePhoto = useCallback(async () => {
    if (!canonReady) {
      setStatus('Hubungkan Canon dulu sebelum ambil foto');
      return;
    }

    setBusy(true);
    for (let i = COUNTDOWN_SECONDS; i >= 1; i--) {
      setCountdown(i);
      await new Promise(r => setTimeout(r, 1000));
    }
    setCountdown(null);

    try {
      const ok = await CanonUsb.triggerShutter();
      if (ok) {
        setStatus('Foto diambil!');
        navigation.navigate('PreviewScreen', {photo: {path: ''}});
      } else {
        setStatus(
          'Shutter PTP belum aktif — USB Canon OK, implementasi capture berikutnya',
        );
      }
    } catch (error) {
      console.error('triggerShutter error:', error);
      setStatus(`Gagal ambil foto: ${error.message ?? error}`);
    } finally {
      setBusy(false);
    }
  }, [canonReady, navigation]);

  return (
    <View style={styles.container}>
      <View style={styles.previewBox}>
        {hdmiPreviewEnabled && UvcCamera && !hdmiError && (
          <ErrorBoundary
            onError={error => {
              console.error('UvcCamera crash:', error);
              setHdmiError(error?.message ?? 'HDMI preview error');
              setHdmiPreviewEnabled(false);
              setStatus(
                'HDMI preview gagal — cek capture card via USB hub, lalu coba lagi',
              );
            }}>
            <UvcCamera
              ref={uvcRef}
              style={StyleSheet.absoluteFill}
              rotation={0}
              ratio="16:9"
              onCameraReady={() => {
                setHdmiReady(true);
                setStatus(prev =>
                  prev.includes('Canon siap')
                    ? prev
                    : 'HDMI preview aktif — hubungkan Canon untuk shutter',
                );
              }}
              onMountError={e => {
                console.error('UvcCamera mount error:', e);
                setHdmiError(e?.message ?? 'mount error');
                setHdmiPreviewEnabled(false);
                setStatus(
                  'HDMI preview gagal — cek capture card via USB hub',
                );
              }}
            />
          </ErrorBoundary>
        )}

        {countdown !== null && (
          <View style={styles.countdownOverlay}>
            <Text style={styles.countdownText}>{countdown}</Text>
          </View>
        )}

        {(!hdmiPreviewEnabled || !hdmiReady) && (
          <View style={styles.previewPlaceholder}>
            <Text style={styles.placeholderText}>
              {!UvcCamera
                ? 'Modul HDMI preview tidak tersedia di build ini.'
                : hdmiError
                  ? `HDMI error: ${hdmiError}\nTap "Nyalakan HDMI Preview" untuk coba lagi.`
                  : 'HDMI preview belum aktif.\nColok capture card ke USB hub, lalu tap "Nyalakan HDMI Preview".\n(Izin Canon ≠ izin capture card)'}
              {'\n\n'}
              (mini HDMI kamera → capture card → USB hub → tablet)
            </Text>
          </View>
        )}
      </View>

      <Text style={styles.status}>{status}</Text>
      {busy && <ActivityIndicator color="#fff" style={styles.spinner} />}

      <View style={styles.controls}>
        <TouchableOpacity
          style={[styles.button, styles.buttonSecondary]}
          onPress={onEnableHdmiPreview}
          disabled={busy || !UvcCamera || hdmiPreviewEnabled}>
          <Text style={styles.buttonText}>
            {hdmiPreviewEnabled ? 'HDMI Aktif' : 'Nyalakan HDMI Preview'}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.button, styles.buttonSecondary]}
          onPress={onConnectCanon}
          disabled={busy}>
          <Text style={styles.buttonText}>Hubungkan Canon</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.button, !canonReady && styles.buttonDisabled]}
          onPress={takePhoto}
          disabled={!canonReady || busy}>
          <Text style={styles.buttonText}>Ambil Foto</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#111',
  },
  previewBox: {
    flex: 1,
    backgroundColor: '#000',
    overflow: 'hidden',
  },
  previewPlaceholder: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#1a1a1a',
    padding: 24,
  },
  placeholderText: {
    color: '#888',
    textAlign: 'center',
    fontSize: 14,
    lineHeight: 22,
  },
  countdownOverlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.35)',
  },
  countdownText: {
    color: '#fff',
    fontSize: 96,
    fontWeight: '800',
  },
  status: {
    color: '#ccc',
    fontSize: 13,
    textAlign: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  spinner: {
    marginBottom: 4,
  },
  controls: {
    flexDirection: 'column',
    padding: 16,
    gap: 12,
  },
  button: {
    flex: 1,
    backgroundColor: '#c0392b',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  buttonSecondary: {
    backgroundColor: '#444',
  },
  buttonDisabled: {
    opacity: 0.4,
  },
  buttonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
});
