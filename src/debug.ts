import { requireNativeModule } from 'expo-modules-core';

import { ScannerMetrics } from './types';

type DebugNativeModule = {
  getScannerMetrics: () => Promise<ScannerMetrics>;
};

const nativeModule = requireNativeModule<DebugNativeModule>('ExpoLogisticsScanner');

/**
 * @internal Debug only — not part of the public API contract.
 * Returns internal scanner metrics from the native layer.
 */
export async function getScannerMetrics(): Promise<ScannerMetrics> {
  return nativeModule.getScannerMetrics();
}
