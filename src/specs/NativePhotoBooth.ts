import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

/**
 * Post-capture booth: MediaStore import, frame composite, pending handoff to JS.
 * Implementation: android/.../booth/PhotoBoothModule.kt
 */
export interface Spec extends TurboModule {
  /** Photo URI captured after the last shutter, or "". */
  consumePendingImport(): Promise<string>;
  /** Newest gallery image from the last 30 minutes, preferring Canon albums. */
  latestImage(): Promise<string>;
  /** JSON array of {id, name}. */
  listFrames(): Promise<string>;
  /**
   * file:// JPEG of the photo with the frame drawn on top.
   * layoutJson is "" for full-bleed frames, or the slot sheet from src/config/frames.js.
   * qrMatrix is "" or "count:0101..." bits drawn into qrSlot.
   */
  compose(
    photoUri: string,
    frameId: string,
    maxEdge: number,
    layoutJson: string,
    qrMatrix: string,
  ): Promise<string>;
}

export default TurboModuleRegistry.get<Spec>('PhotoBooth');
