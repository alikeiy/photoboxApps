import React, {useCallback, useEffect, useState} from 'react';
import {
  ActivityIndicator,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import Share from 'react-native-share';
import {uploadComposite} from '../src/cloud/uploadComposite';
import {frameById, layoutPayload} from '../src/config/frames';
import {messageFor, openWhatsApp} from '../src/share/openWhatsApp';
import {encodeQrMatrix} from '../src/share/qrMatrix';
import PhotoBooth from '../src/native/PhotoBooth';
import {colors, font} from '../src/theme/theme';
import QrCode from './QrCode';

export default function BoothShareScreen({route, navigation}) {
  const imageUri = route.params?.imageUri ?? '';
  const frameName = route.params?.frameName ?? '';
  const photoUri = route.params?.photoUri ?? '';
  const frameId = route.params?.frameId ?? '';
  const [displayUri, setDisplayUri] = useState(imageUri);
  const [url, setUrl] = useState('');
  const [message, setMessage] = useState('Mengunggah foto…');
  const [busy, setBusy] = useState(false);

  const upload = useCallback(async () => {
    if (!imageUri) {
      setMessage('Foto akhir tidak ada.');
      return;
    }
    setBusy(true);
    setUrl('');
    setMessage('Mengunggah foto…');
    try {
      const next = await uploadComposite(imageUri);
      setUrl(next);
      const frame = frameById(frameId);
      if (frame?.qrSlot && photoUri) {
        try {
          const stamped = await PhotoBooth.compose(
            photoUri,
            frameId,
            2700,
            layoutPayload(frameId),
            encodeQrMatrix(next),
          );
          setDisplayUri(stamped);
        } catch (error) {
          console.warn('QR slot compose failed', error);
        }
      }
      setMessage('Tamu bisa memindai QR atau membuka WhatsApp.');
    } catch (error) {
      setMessage(error?.message ?? 'Upload gagal');
    } finally {
      setBusy(false);
    }
  }, [frameId, imageUri, photoUri]);

  useEffect(() => {
    upload();
  }, [upload]);

  const onWhatsApp = useCallback(async () => {
    try {
      await openWhatsApp(url);
    } catch (error) {
      setMessage(error?.message ?? 'WhatsApp tidak terbuka');
    }
  }, [url]);

  const onShareImage = useCallback(async () => {
    try {
      await Share.open({
        url: displayUri,
        type: 'image/jpeg',
        failOnCancel: false,
      });
    } catch (error) {
      setMessage(error?.message ?? 'Gagal membagikan gambar');
    }
  }, [displayUri]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.row}>
        <Image source={{uri: displayUri}} style={styles.photo} />
        <View style={styles.side}>
          <Text style={styles.title}>Bagikan foto</Text>
          {!!frameName && <Text style={styles.meta}>{frameName}</Text>}
          <Text style={styles.message}>{message}</Text>
          {busy && <ActivityIndicator color={colors.primaryDeep} />}
          {!!url && (
            <View style={styles.qrWrap}>
              <QrCode value={url} size={200} />
              <Text style={styles.link} numberOfLines={3}>
                {messageFor(url)}
              </Text>
            </View>
          )}
          <TouchableOpacity
            style={[styles.button, !url && styles.buttonDisabled]}
            onPress={onWhatsApp}
            disabled={!url}>
            <Text style={styles.buttonText}>Kirim lewat WhatsApp</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={onShareImage}>
            <Text style={styles.buttonText}>Bagikan gambar</Text>
          </TouchableOpacity>
          {!url && (
            <TouchableOpacity
              style={[styles.button, styles.buttonQuiet]}
              onPress={upload}
              disabled={busy}>
              <Text style={[styles.buttonText, styles.buttonTextQuiet]}>
              Coba unggah lagi
            </Text>
            </TouchableOpacity>
          )}
          <TouchableOpacity
            style={[styles.button, styles.buttonQuiet]}
            onPress={() => navigation.navigate('TriggerSetup')}>
            <Text style={[styles.buttonText, styles.buttonTextQuiet]}>
              Foto berikutnya
            </Text>
          </TouchableOpacity>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    backgroundColor: colors.background,
    padding: 16,
  },
  row: {
    flex: 1,
    flexDirection: 'row',
    gap: 16,
  },
  photo: {
    flex: 1.2,
    minHeight: 280,
    backgroundColor: colors.backgroundDeep,
    borderRadius: 28,
    borderWidth: 1,
    borderColor: colors.border,
    resizeMode: 'contain',
  },
  side: {
    flex: 1,
    gap: 10,
    justifyContent: 'center',
  },
  title: {
    color: colors.text,
    fontFamily: font.bold,
    fontSize: 30,
  },
  meta: {
    color: colors.primaryDeep,
    fontFamily: font.semibold,
    fontSize: 14,
  },
  message: {
    color: colors.textMuted,
    fontFamily: font.regular,
    fontSize: 15,
    lineHeight: 22,
  },
  qrWrap: {
    alignItems: 'flex-start',
    gap: 8,
  },
  link: {
    color: colors.textMuted,
    fontFamily: font.regular,
    fontSize: 13,
  },
  button: {
    backgroundColor: colors.primary,
    borderRadius: 28,
    paddingVertical: 14,
    paddingHorizontal: 18,
    alignItems: 'center',
  },
  buttonQuiet: {
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
  },
  buttonDisabled: {
    opacity: 0.45,
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
});
