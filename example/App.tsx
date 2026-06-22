import { ScannerView, ScanResult, ScannerError } from 'expo-logistics-scanner';
import { useCallback, useEffect, useState } from 'react';
import { Button, PermissionsAndroid, Platform, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

type PermissionState = 'pending' | 'granted' | 'denied';

export default function App() {
  const [permissionState, setPermissionState] = useState<PermissionState>('pending');
  const [torchEnabled, setTorchEnabled] = useState(false);
  const [lastScan, setLastScan] = useState<ScanResult | null>(null);
  const [lastError, setLastError] = useState<ScannerError | null>(null);

  useEffect(() => {
    async function requestCameraPermission() {
      if (Platform.OS === 'ios') {
        setPermissionState('granted');
        return;
      }

      if (Platform.OS !== 'android') {
        setPermissionState('denied');
        return;
      }

      const granted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.CAMERA, {
        title: 'Camera permission',
        message: 'Camera access is required to scan barcodes.',
        buttonPositive: 'Allow',
        buttonNegative: 'Deny',
      });

      setPermissionState(granted === PermissionsAndroid.RESULTS.GRANTED ? 'granted' : 'denied');
    }

    requestCameraPermission();
  }, []);

  const handleScan = useCallback((result: ScanResult) => {
    setLastScan(result);
    setLastError(null);
  }, []);

  const handleError = useCallback((error: ScannerError) => {
    setLastError(error);
  }, []);

  if (permissionState === 'pending') {
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.message}>Requesting camera permission...</Text>
      </SafeAreaView>
    );
  }

  if (permissionState === 'denied') {
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.message}>Camera permission is required to use the scanner.</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScannerView
        key="scanner"
        style={styles.scanner}
        torch={torchEnabled}
        duplicateTimeout={750}
        onScan={handleScan}
        onError={handleError}
      />
      <View style={styles.controls}>
        <Button
          title={torchEnabled ? 'Turn torch off' : 'Turn torch on'}
          onPress={() => setTorchEnabled((current) => !current)}
        />
        <Text style={styles.label}>Last scan</Text>
        <Text style={styles.value}>{lastScan?.value ?? 'No barcode scanned yet'}</Text>
        {lastScan ? (
          <Text style={styles.meta}>
            {lastScan.format} · {new Date(lastScan.timestamp).toLocaleTimeString()}
          </Text>
        ) : null}
        {lastError ? (
          <>
            <Text style={styles.label}>Last error</Text>
            <Text style={styles.error}>
              {lastError.code}: {lastError.message}
            </Text>
          </>
        ) : null}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#000',
    flex: 1,
  },
  controls: {
    backgroundColor: '#111',
    gap: 8,
    padding: 16,
  },
  error: {
    color: '#f87171',
    fontSize: 14,
  },
  label: {
    color: '#aaa',
    fontSize: 14,
    marginTop: 8,
  },
  message: {
    color: '#fff',
    fontSize: 16,
    margin: 24,
    textAlign: 'center',
  },
  meta: {
    color: '#888',
    fontSize: 12,
  },
  scanner: {
    flex: 1,
  },
  value: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
});
