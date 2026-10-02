/**
 * Cloud delivery for the finished booth JPEG.
 *
 * uploadUrl: POST multipart. Leave empty until the server is ready.
 * The body field name is uploadField (default "file").
 * extraFields are appended as text parts (Cloudinary upload_preset, etc.).
 *
 * Accepted responses:
 * - JSON { "url": "https://..." } or secure_url / publicUrl / data.url
 * - a plain text URL
 *
 * Cloudinary example:
 *   uploadUrl: 'https://api.cloudinary.com/v1_1/YOUR_CLOUD/image/upload'
 *   extraFields: { upload_preset: 'YOUR_UNSIGNED_PRESET' }
 *
 * whatsappPhone: country code + number, digits only. Empty opens WhatsApp
 * without a recipient so the operator can pick the guest's chat.
 */
const boothConfig = {
  uploadUrl: '',
  uploadField: 'file',
  extraFields: {},
  uploadHeaders: {},
  whatsappPhone: '',
  shareMessage: 'Foto photobooth kamu sudah siap: ',
};

export default boothConfig;
