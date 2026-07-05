import React, {useCallback, useEffect, useState} from 'react';
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import CanonUsb from '../src/native/CanonUsb';

export default function CameraScreen() {
  const [status, setStatus] = useState('Memulai USB…');
  const [usbReady, setUsbReady] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const ok = await CanonUsb.initializeUsb();
        setUsbReady(ok);
        setStatus(
          ok
            ? 'USB Host siap — colokkan Canon EOS M10 via OTG'
            : 'USB Host tidak didukung di tablet ini',
        );
      } catch (error) {
        console.error('initializeUsb error:', error);
        setStatus('Gagal inisialisasi USB');
      }
    })();
  }, []);

  const onRequestPermission = useCallback(async () => {
    setBusy(true);
    try {
      const granted = await CanonUsb.requestCameraPermission();
      setStatus(
        granted
          ? 'Izin USB diberikan'
          : 'Izin USB ditolak (skeleton — belum diimplementasi)',
      );
    } catch (error) {
      console.error('requestCameraPermission error:', error);
      setStatus('Gagal meminta izin USB');
    } finally {
      setBusy(false);
    }
  }, []);

  const onTriggerShutter = useCallback(async () => {
    setBusy(true);
    try {
      const ok = await CanonUsb.triggerShutter();
      setStatus(
        ok
          ? 'Shutter terpicu'
          : 'Shutter gagal (skeleton — belum diimplementasi)',
      );
    } catch (error) {
      console.error('triggerShutter error:', error);
      setStatus('Gagal memicu shutter');
    } finally {
      setBusy(false);
    }
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Canon USB Photo Booth</Text>
      <Text style={styles.status}>{status}</Text>

      {busy && <ActivityIndicator color="#fff" style={styles.spinner} />}

      <TouchableOpacity
        style={[styles.button, !usbReady && styles.buttonDisabled]}
        onPress={onRequestPermission}
        disabled={!usbReady || busy}>
        <Text style={styles.buttonText}>Minta Izin USB</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, !usbReady && styles.buttonDisabled]}
        onPress={onTriggerShutter}
        disabled={!usbReady || busy}>
        <Text style={styles.buttonText}>Trigger Shutter</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#111',
    justifyContent: 'center',
    padding: 24,
  },
  title: {
    color: '#fff',
    fontSize: 22,
    fontWeight: '700',
    marginBottom: 16,
    textAlign: 'center',
  },
  status: {
    color: '#ccc',
    fontSize: 14,
    marginBottom: 24,
    textAlign: 'center',
  },
  spinner: {
    marginBottom: 16,
  },
  button: {
    backgroundColor: '#c0392b',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.4,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
