Expo Logistics Scanner MVP

Objective

Build the smallest possible production-quality barcode scanner capable of scanning CODE_128 barcodes using the device camera on Android.

The MVP should validate:

* Expo Module architecture
* Camera integration
* Native barcode decoding
* Event emission
* React Native API design

Do not build future features during MVP implementation.

Scope

Included

Android only

CameraX preview

ML Kit barcode scanning

CODE_128 support

React Native ScannerView component

Scan event emission

Start / Stop lifecycle

Torch support

Excluded

iOS

Zebra integration

Honeywell integration

ROI selection

Duplicate filtering

Batch events

Custom overlays

Scan history

Sound effects

Haptics

Multiple barcode formats

Configuration screens

Performance optimizations beyond basic best practices

Public API

Component

<ScannerView
  onScan={handleScan}
  torch={false}
/>

Scan Result

interface ScanResult {
  value: string
  format: string
  timestamp: number
}

Event

onScan(result)

Emit one event per successful barcode detection.

Folder Structure

src/
ScannerView.tsx
types.ts
index.ts

android/
scanner/
CameraScannerProvider.kt
BarcodeAnalyzer.kt
ScannerView.kt

Implementation Requirements

Camera

Use CameraX PreviewView.

Analysis

Use CameraX ImageAnalysis.

Barcode Detection

Use ML Kit Barcode Scanner.

Restrict detection to:

CODE_128

only.

Frame Processing

Use a “process latest frame only” strategy.

Do not queue frames.

If analyzer is busy, drop incoming frames.

Event Delivery

Emit scan results to JavaScript using Expo Modules events.

Error Handling

Handle:

* Camera permission denied
* Camera unavailable
* ML Kit initialization failure

without crashing.

Acceptance Criteria

1. Android application launches successfully.
2. ScannerView displays camera preview.
3. CODE_128 barcode can be scanned.
4. onScan callback receives barcode value.
5. Continuous scanning works.
6. No crashes during repeated scans.
7. Library works inside the example Expo application.

Post-MVP Roadmap

Phase 2

* Duplicate filtering
* ROI support
* Performance benchmarking

Phase 3

* iOS implementation

Phase 4

* Zebra provider

Phase 5

* Honeywell provider

Phase 6

* Additional barcode formats