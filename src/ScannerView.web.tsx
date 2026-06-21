import { StyleSheet, Text, View } from 'react-native';

import { ScannerViewProps } from './types';

export function ScannerView(_props: ScannerViewProps) {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>ScannerView is Android-only</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    backgroundColor: '#111',
    flex: 1,
    justifyContent: 'center',
  },
  text: {
    color: '#fff',
    fontSize: 16,
  },
});
