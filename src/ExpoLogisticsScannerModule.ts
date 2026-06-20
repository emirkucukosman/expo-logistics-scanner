import { NativeModule, requireNativeModule } from 'expo';

import { ExpoLogisticsScannerModuleEvents } from './ExpoLogisticsScanner.types';

declare class ExpoLogisticsScannerModule extends NativeModule<ExpoLogisticsScannerModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

export default requireNativeModule<ExpoLogisticsScannerModule>('ExpoLogisticsScanner');
