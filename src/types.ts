import { ViewProps } from 'react-native';

export type ScanResult = {
  value: string;
  format: string;
  timestamp: number;
};

export type ScannerErrorCode =
  | 'permission_denied'
  | 'camera_unavailable'
  | 'decoder_failure'
  | 'interrupted';

export type ScannerError = {
  code: ScannerErrorCode;
  message: string;
};

export type ScannerViewProps = ViewProps & {
  torch?: boolean;
  duplicateTimeout?: number;
  onScan?: (result: ScanResult) => void;
  onError?: (error: ScannerError) => void;
};

export type NativeScanEvent = {
  nativeEvent: ScanResult;
};

export type NativeErrorEvent = {
  nativeEvent: ScannerError;
};

export type ScannerMetrics = {
  cameraStartupMs: number;
  lastDecodeLatencyMs: number;
  scanCount: number;
};
