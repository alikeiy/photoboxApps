import {Linking} from 'react-native';
import boothConfig from '../config/booth';

export function messageFor(url) {
  const prefix = boothConfig.shareMessage || 'Foto photobooth kamu sudah siap: ';
  return `${prefix}${url}`;
}

export function openWhatsApp(url) {
  const text = encodeURIComponent(messageFor(url));
  const phone = String(boothConfig.whatsappPhone || '').replace(/[^\d]/g, '');
  const link = phone
    ? `https://api.whatsapp.com/send?phone=${phone}&text=${text}`
    : `https://api.whatsapp.com/send?text=${text}`;
  return Linking.openURL(link);
}
