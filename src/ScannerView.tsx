import { requireNativeViewManager } from 'expo-modules-core';

import { NativeErrorEvent, NativeScanEvent, ScannerViewProps } from './types';

export type { ScanResult, ScannerError, ScannerErrorCode, ScannerViewProps } from './types';

type NativeScannerViewProps = Omit<ScannerViewProps, 'onScan' | 'onError'> & {
  onScan?: (event: NativeScanEvent) => void;
  onError?: (event: NativeErrorEvent) => void;
};

const NativeScannerView = requireNativeViewManager<NativeScannerViewProps>('ExpoLogisticsScanner');

export function ScannerView({
  onScan,
  onError,
  torch = false,
  duplicateTimeout = 0,
  ...rest
}: ScannerViewProps) {
  return (
    <NativeScannerView
      {...rest}
      torch={torch}
      duplicateTimeout={duplicateTimeout}
      onScan={onScan ? (event: NativeScanEvent) => onScan(event.nativeEvent) : undefined}
      onError={onError ? (event: NativeErrorEvent) => onError(event.nativeEvent) : undefined}
    />
  );
}
