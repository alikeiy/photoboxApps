import React, {useCallback, useState} from 'react';
import {
  AppState,
  PermissionsAndroid,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {useFocusEffect} from '@react-navigation/native';
import PhotoBooth from '../src/native/PhotoBooth';
import ShutterTrigger from '../src/native/ShutterTrigger';
import {colors, font, shadow} from '../src/theme/theme';

function describe(statusNow) {
  if (!ShutterTrigger.isAvailable()) {
    return 'Modul native belum ada. Rebuild aplikasi Android.';
  }
  if (!statusNow) return 'Memeriksa izin…';
  if (statusNow.anchorValid) {
    return `Pin di ${Math.round(statusNow.x)}, ${Math.round(statusNow.y)} — ${
      statusNow.locked ? 'terkunci' : 'belum dikunci'
    }`;
  }
  return 'Pin belum sah. Landscape, lalu geser gagang ke tombol shutter.';
}

async function ensurePhotoReadPermission() {
  if (Platform.OS !== 'android') return true;
  const permission =
    Platform.Version >= 33
      ? PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES
      : PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE;
  const granted = await PermissionsAndroid.request(permission);
  return granted === PermissionsAndroid.RESULTS.GRANTED;
}

async function ensureNotificationPermission() {
  if (Platform.OS !== 'android' || Platform.Version < 33) return true;
  const granted = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
  );
  return granted === PermissionsAndroid.RESULTS.GRANTED;
}

/**
 * Setup for tapping the Canon Camera Connect shutter from a volume key
 * or Bluetooth remote, while that app stays in front of the tablet.
 */
export default function TriggerSetupScreen({navigation}) {
  const [status, setStatus] = useState(null);
  const [message, setMessage] = useState('Memeriksa izin…');
  const [busy, setBusy] = useState(false);

  const loadStatus = useCallback(async () => {
    if (!ShutterTrigger.isAvailable()) {
      setStatus(null);
      return null;
    }
    const [
      overlay,
      accessibility,
      battery,
      running,
      locked,
      anchorValid,
      landscape,
      x,
      y,
    ] = await Promise.all([
      ShutterTrigger.canDrawOverlays(),
      ShutterTrigger.isAccessibilityEnabled(),
      ShutterTrigger.isIgnoringBatteryOptimizations(),
      ShutterTrigger.isTriggerRunning(),
      ShutterTrigger.isPinLocked(),
      ShutterTrigger.isAnchorValid(),
      ShutterTrigger.isDisplayLandscape(),
      ShutterTrigger.getAnchorX(),
      ShutterTrigger.getAnchorY(),
    ]);
    const next = {
      overlay,
      accessibility,
      battery,
      running,
      locked,
      anchorValid,
      landscape,
      x,
      y,
    };
    setStatus(next);
    return next;
  }, []);

  useFocusEffect(
    useCallback(() => {
      let alive = true;
      const pull = async () => {
        try {
          const next = await loadStatus();
          if (alive) setMessage(describe(next));
        } catch (error) {
          if (alive) setMessage(error?.message ?? 'Gagal membaca status pemicu');
        }
      };
      pull();
      const sub = AppState.addEventListener('change', next => {
        if (next === 'active') pull();
      });
      return () => {
        alive = false;
        sub.remove();
      };
    }, [loadStatus]),
  );

  const run = useCallback(
    async action => {
      setBusy(true);
      try {
        const note = await action();
        const next = await loadStatus();
        setMessage(typeof note === 'string' ? note : describe(next));
      } catch (error) {
        setMessage(error?.message ?? 'Gagal');
      } finally {
        setBusy(false);
      }
    },
    [loadStatus],
  );

  const onLatestPhoto = useCallback(() => {
    run(async () => {
      const photos = await ensurePhotoReadPermission();
      if (!photos) {
        return 'Izinkan semua foto dulu, supaya foto Canon bisa dibaca.';
      }
      const uri = await PhotoBooth.latestImage();
      if (!uri) {
        return 'Belum ada foto baru di galeri (30 menit terakhir).';
      }
      navigation.navigate('FrameSelect', {photoUri: uri});
      return 'Membuka foto Canon terbaru.';
    });
  }, [navigation, run]);

  const onStart = useCallback(() => {
    run(async () => {
      await ensureNotificationPermission();
      const photos = await ensurePhotoReadPermission();
      const started = await ShutterTrigger.startTrigger();
      if (!started) {
        return 'Izin tampil di atas aplikasi lain belum diberikan.';
      }
      return photos
        ? 'Pin aktif. Setelah shutter, foto Canon terbuka di pilihan frame. Pilih "Izinkan semua" jika Android menanyakan akses foto.'
        : 'Pin aktif, tetapi akses foto ditolak. Tanpa izin itu, foto Canon tidak bisa masuk ke frame.';
    });
  }, [run]);

  const onToggleLock = useCallback(() => {
    run(async () => {
      const locked = status?.locked === true;
      await ShutterTrigger.setPinLocked(!locked);
    });
  }, [run, status?.locked]);

  const onTest = useCallback(() => {
    run(async () => {
      const ok = await ShutterTrigger.fireTestTap();
      return ok
        ? 'Ketukan terkirim ke titik pin.'
        : 'Ketukan gagal. Aktifkan aksesibilitas Photobox, kunci landscape, dan taruh pin dulu.';
    });
  }, [run]);

  const onAutostart = useCallback(() => {
    run(async () => {
      const opened = await ShutterTrigger.openOemAutostartSettings();
      return opened
        ? 'Izinkan autostart untuk photoboxTemp, lalu kembali ke sini.'
        : 'Layar autostart Realme tidak terbuka. Buka Setelan > Aplikasi > photoboxTemp > Autostart, dan set baterai ke Tidak dibatasi.';
    });
  }, [run]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.kicker}>KeiyBooth</Text>
      <Text style={styles.title}>Canon Camera Connect</Text>
      <Text style={styles.body}>
        Kamera tetap di aplikasi Canon. Tablet ini menaruh pin di atas tombol
        shutter, lalu mengetuk titik itu saat volume atau remote ditekan.
      </Text>

      <View style={styles.card}>
        <Row label="Landscape" ok={status?.landscape} />
        <Row label="Tampil di atas aplikasi" ok={status?.overlay} />
        <Row label="Aksesibilitas Photobox" ok={status?.accessibility} />
        <Row label="Baterai tidak dibatasi" ok={status?.battery} />
        <Row label="Pin berjalan" ok={status?.running} />
        <Row label="Koordinat pin sah" ok={status?.anchorValid} />
      </View>

      <Text style={styles.status}>{message}</Text>

      <TouchableOpacity
        style={styles.button}
        disabled={busy}
        onPress={() => run(() => ShutterTrigger.openOverlaySettings())}>
        <Text style={styles.buttonText}>1. Izinkan tampil di atas aplikasi</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        disabled={busy}
        onPress={() => run(() => ShutterTrigger.openAccessibilitySettings())}>
        <Text style={styles.buttonText}>2. Aktifkan aksesibilitas Photobox</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        disabled={busy}
        onPress={() =>
          run(() => ShutterTrigger.requestIgnoreBatteryOptimizations())
        }>
        <Text style={styles.buttonText}>3. Bebaskan dari penghemat baterai</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        disabled={busy}
        onPress={onAutostart}>
        <Text style={styles.buttonText}>4. Autostart Realme</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={[styles.button, styles.buttonPrimary]}
        disabled={busy}
        onPress={onStart}>
        <Text style={styles.buttonText}>Mulai pin shutter</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        disabled={busy}
        onPress={onLatestPhoto}>
        <Text style={styles.buttonText}>Foto Canon terbaru</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        disabled={busy || !status?.running}
        onPress={onToggleLock}>
        <Text style={styles.buttonText}>
          {status?.locked ? 'Buka kunci pin' : 'Kunci pin'}
        </Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        disabled={busy}
        onPress={onTest}>
        <Text style={styles.buttonText}>Tes ketuk</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={[styles.button, styles.buttonQuiet]}
        disabled={busy}
        onPress={() => run(() => ShutterTrigger.stopTrigger())}>
        <Text style={[styles.buttonText, styles.buttonTextQuiet]}>Berhenti</Text>
      </TouchableOpacity>

      <Text style={styles.hint}>
        Kunci rotasi tablet di landscape sebelum membuka Canon Camera Connect.
        Hijau berarti pin terkunci dan ketukan aktif. Merah berarti masih bisa
        digeser. Oranye berarti layar berubah — geser pin lagi. Gagang GESER
        adalah satu-satunya area yang menangkap sentuhan; lingkaran pin
        meneruskan sentuhan ke aplikasi kamera.
      </Text>
    </ScrollView>
  );
}

function Row({label, ok}) {
  return (
    <Text style={styles.row}>
      {ok ? '●' : '○'} {label}
    </Text>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    backgroundColor: colors.background,
    padding: 20,
    gap: 12,
  },
  kicker: {
    color: colors.primaryDeep,
    fontFamily: font.semibold,
    fontSize: 16,
    letterSpacing: 0.4,
  },
  title: {
    color: colors.text,
    fontFamily: font.bold,
    fontSize: 30,
  },
  body: {
    color: colors.textMuted,
    fontFamily: font.regular,
    fontSize: 15,
    lineHeight: 22,
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 16,
    gap: 8,
    ...shadow,
  },
  row: {
    color: colors.text,
    fontFamily: font.regular,
    fontSize: 16,
  },
  status: {
    color: colors.text,
    fontFamily: font.regular,
    fontSize: 14,
    lineHeight: 20,
  },
  button: {
    backgroundColor: colors.primary,
    borderRadius: 28,
    paddingVertical: 14,
    paddingHorizontal: 18,
    alignItems: 'center',
  },
  buttonPrimary: {
    backgroundColor: colors.primaryDeep,
  },
  buttonQuiet: {
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
  },
  buttonText: {
    color: colors.white,
    fontFamily: font.semibold,
    fontSize: 16,
    textAlign: 'center',
  },
  buttonTextQuiet: {
    color: colors.text,
  },
  hint: {
    color: colors.textMuted,
    fontFamily: font.regular,
    fontSize: 13,
    lineHeight: 20,
    marginTop: 4,
    marginBottom: 12,
  },
});
