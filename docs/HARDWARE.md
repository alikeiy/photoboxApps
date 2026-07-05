# Hardware Setup — Photo Booth Klasik (HDMI + USB)

## Arsitektur

```
Canon EOS M10
    │
    ├── mini HDMI ──► HDMI Capture Card ──USB──┐
    │                                         │
    └── micro USB (PTP) ──────────────────────┼──► Powered USB Hub ──► Realme Pad Mini
                                              │
                                    (satu port USB-C tablet)
```

| Jalur | Fungsi | Protokol di app |
|-------|--------|-----------------|
| **HDMI → Capture Card** | Live preview di layar | `UvcCamera` (UVC) |
| **USB → Canon** | Trigger shutter + download foto | `CanonUsbModule` (PTP) |

## Yang dibutuhkan

1. **Canon EOS M10** — mode USB: **Photo transfer / PTP**
2. **Kabel mini HDMI** — dari kamera ke capture card
3. **HDMI USB Capture Card** — contoh: generic UVC dongle, Elgato Cam Link, dll.
4. **Powered USB Hub** — wajib; tablet tidak cukup power untuk kamera + capture card
5. **Kabel USB** — Canon micro USB ke hub
6. **OTG adapter USB-C** — hub ke Realme Pad Mini

## Di aplikasi

1. Buka app → **HDMI preview** muncul otomatis (capture card)
2. Tap **Hubungkan Canon** → izin USB untuk kamera PTP
3. Tap **Ambil Foto** → countdown → shutter via USB

## Catatan

- **Jangan** colok Canon langsung ke tablet untuk preview — Canon bukan UVC webcam.
- Preview **hanya** dari HDMI capture card.
- Shutter **hanya** dari USB PTP (belum full PTP capture — Phase 2).
