import boothConfig from '../config/booth';

function urlFromBody(text) {
  const trimmed = text.trim();
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed;
  }
  const json = JSON.parse(trimmed);
  const url = json.url || json.secure_url || json.publicUrl || json.data?.url;
  if (typeof url !== 'string' || !url.startsWith('http')) {
    throw new Error('Respons upload tidak berisi URL publik.');
  }
  return url;
}

/**
 * Uploads the composed JPEG and returns its public URL.
 * Does not set Content-Type so fetch can add the multipart boundary.
 */
export async function uploadComposite(fileUri) {
  if (!boothConfig.uploadUrl) {
    throw new Error(
      'Alamat upload masih kosong. Isi uploadUrl di src/config/booth.js.',
    );
  }

  const body = new FormData();
  body.append(boothConfig.uploadField || 'file', {
    uri: fileUri,
    type: 'image/jpeg',
    name: `keiybooth-${Date.now()}.jpg`,
  });
  const extra = boothConfig.extraFields || {};
  Object.keys(extra).forEach(key => {
    if (extra[key] != null) body.append(key, String(extra[key]));
  });

  const response = await fetch(boothConfig.uploadUrl, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      ...(boothConfig.uploadHeaders || {}),
    },
    body,
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(text || `Upload gagal (${response.status})`);
  }
  return urlFromBody(text);
}
