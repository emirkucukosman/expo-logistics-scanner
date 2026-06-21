import { ViewProps } from 'react-native';

export type ScanResult = {
  value: string;
  format: string;
  timestamp: number;
};

export type ScannerViewProps = ViewProps & {
  torch?: boolean;
  onScan?: (result: ScanResult) => void;
};

export type NativeScanEvent = {
  nativeEvent: ScanResult;
};
