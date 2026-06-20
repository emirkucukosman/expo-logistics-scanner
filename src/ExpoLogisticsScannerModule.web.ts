import { registerWebModule, NativeModule } from 'expo';

import { ExpoLogisticsScannerModuleEvents } from './ExpoLogisticsScanner.types';

// ExpoLogisticsScannerModule is not available on the web platform.
class ExpoLogisticsScannerModule extends NativeModule<ExpoLogisticsScannerModuleEvents> {}

export default registerWebModule(ExpoLogisticsScannerModule, 'ExpoLogisticsScannerModule');
