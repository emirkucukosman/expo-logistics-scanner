// Reexport the native module. On web, it will be resolved to ExpoLogisticsScannerModule.web.ts
// and on native platforms to ExpoLogisticsScannerModule.ts
export { default } from './ExpoLogisticsScannerModule';
export * from './ExpoLogisticsScanner.types';
