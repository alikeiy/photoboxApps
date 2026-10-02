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
import PhotoBooth from '../src/native/PhotoBooth';
import {layoutPayload} from '../src/config/frames';
import {colors, font} from '../src/theme/theme';

const PREVIEW_EDGE = 1400;
const FINAL_EDGE = 2700;

export default function FrameSelectScreen({route, navigation}) {
  const photoUri = route.params?.photoUri ?? '';
  const [frames, setFrames] = useState([]);
  const [frameId, setFrameId] = useState('classic');
  const [previewUri, setPreviewUri] = useState('');
  const [message, setMessage] = useState('Memuat frame…');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const list = await PhotoBooth.listFrames();
        if (!alive) return;
        setFrames(list);
        if (list[0]) setFrameId(list[0].id);
      } catch (error) {
        if (alive) setMessage(error?.message ?? 'Gagal memuat frame');
      }
    })();
    return () => {
      alive = false;
    };
  }, []);

  const renderPreview = useCallback(
    async id => {
      if (!photoUri) return;
      setBusy(true);
      setMessage('Menyusun preview…');
      try {
        const uri = await PhotoBooth.compose(
          photoUri,
          id,
          PREVIEW_EDGE,
          layoutPayload(id),
          '',
        );
        setPreviewUri(uri);
        setMessage('Pilih frame, lalu lanjut untuk unggah.');
      } catch (error) {
        setMessage(error?.message ?? 'Gagal menyusun frame');
      } finally {
        setBusy(false);
      }
    },
    [photoUri],
  );

  useEffect(() => {
    if (!photoUri || !frameId) return;
    renderPreview(frameId);
  }, [photoUri, frameId, renderPreview]);

  const onContinue = useCallback(async () => {
    if (!photoUri || !frameId) return;
    setBusy(true);
    setMessage('Menyimpan foto akhir…');
    try {
      const imageUri = await PhotoBooth.compose(
        photoUri,
        frameId,
        FINAL_EDGE,
        layoutPayload(frameId),
        '',
      );
      const frame = frames.find(item => item.id === frameId);
      navigation.navigate('BoothShare', {
        imageUri,
        frameName: frame?.name ?? frameId,
        photoUri,
        frameId,
      });
    } catch (error) {
      setMessage(error?.message ?? 'Gagal menyimpan foto');
    } finally {
      setBusy(false);
    }
  }, [frameId, frames, navigation, photoUri]);

  return (
    <View style={styles.container}>
      <View style={styles.previewBox}>
        {previewUri ? (
          <Image source={{uri: previewUri}} style={styles.preview} />
        ) : (
          photoUri && (
            <Image source={{uri: photoUri}} style={styles.preview} />
          )
        )}
        {busy && (
          <View style={styles.busy}>
            <ActivityIndicator color={colors.primaryDeep} size="large" />
          </View>
        )}
      </View>

      <View style={styles.side}>
        <Text style={styles.title}>Pilih frame</Text>
        <Text style={styles.message}>{message}</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          {frames.map(frame => {
            const selected = frame.id === frameId;
            return (
              <TouchableOpacity
                key={frame.id}
                style={[styles.chip, selected && styles.chipSelected]}
                onPress={() => setFrameId(frame.id)}
                disabled={busy}>
                <Text style={[styles.chipText, selected && styles.chipTextSelected]}>
                  {frame.name}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
        <TouchableOpacity
          style={[styles.button, (!previewUri || busy) && styles.buttonDisabled]}
          onPress={onContinue}
          disabled={!previewUri || busy}>
          <Text style={styles.buttonText}>Lanjut bagikan</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.button, styles.buttonQuiet]}
          onPress={() => navigation.navigate('TriggerSetup')}>
          <Text style={[styles.buttonText, styles.buttonTextQuiet]}>Kembali</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'row',
    backgroundColor: colors.background,
  },
  previewBox: {
    flex: 1.4,
    backgroundColor: colors.backgroundDeep,
    margin: 16,
    borderRadius: 28,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: colors.border,
  },
  preview: {
    flex: 1,
    resizeMode: 'contain',
  },
  busy: {
    ...StyleSheet.absoluteFillObject,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255,240,245,0.55)',
  },
  side: {
    flex: 1,
    padding: 20,
    gap: 12,
    justifyContent: 'center',
  },
  title: {
    color: colors.text,
    fontFamily: font.bold,
    fontSize: 30,
  },
  message: {
    color: colors.textMuted,
    fontFamily: font.regular,
    fontSize: 15,
    lineHeight: 22,
  },
  chip: {
    backgroundColor: colors.white,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: colors.border,
    paddingVertical: 14,
    paddingHorizontal: 16,
    marginRight: 10,
  },
  chipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  chipText: {
    color: colors.text,
    fontFamily: font.semibold,
    fontSize: 15,
  },
  chipTextSelected: {
    color: colors.white,
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
