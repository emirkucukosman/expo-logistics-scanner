import { requireNativeViewManager } from 'expo-modules-core';

import { NativeScanEvent, ScannerViewProps } from './types';

export type { ScanResult, ScannerViewProps } from './types';

type NativeScannerViewProps = Omit<ScannerViewProps, 'onScan'> & {
  onScan?: (event: NativeScanEvent) => void;
};

const NativeScannerView = requireNativeViewManager<NativeScannerViewProps>('ExpoLogisticsScanner');

export function ScannerView({ onScan, torch = false, ...rest }: ScannerViewProps) {
  return (
    <NativeScannerView
      {...rest}
      torch={torch}
      onScan={onScan ? (event: NativeScanEvent) => onScan(event.nativeEvent) : undefined}
    />
  );
}
