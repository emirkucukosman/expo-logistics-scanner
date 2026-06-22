API_CONTRACT.md

Public API Contract

Purpose

This document defines the public API that all implementations must support.

The API should remain stable across Android, iOS, Zebra, and Honeywell implementations.

Avoid introducing platform-specific APIs.

ScannerView

<ScannerView
  onScan={handleScan}
  torch={false}
  formats={['code128']}
/>

ScannerView Props

onScan

(result: ScanResult) => void

Called when a barcode is successfully detected.

torch

boolean

Enables device flashlight.

Default:

false

formats

BarcodeFormat[]

Initially:

['code128']

Supported future values:

'code128'
'ean13'
'code39'
'qr'

duplicateTimeout

Planned Milestone 3.

number

Milliseconds.

Example:

duplicateTimeout={750}

Behavior:

Repeated scans of the same barcode within timeout window should be ignored.

Default:

0

Disabled.

roi

Planned Milestone 4.

{
  x: number
  y: number
  width: number
  height: number
}

Normalized coordinates.

Values between:

0-1

ScanResult

interface ScanResult {
  value: string
  format: string
  timestamp: number
}

Future expansion:

interface ScanResult {
  value: string
  format: string
  timestamp: number
  source?: 'camera' | 'zebra' | 'honeywell'
}

Compatibility Requirements

Android implementation:

Must conform.

iOS implementation:

Must conform.

Zebra implementation:

Must conform.

Honeywell implementation:

Must conform.

Applications should not require:

if (Platform.OS === ...)

for normal usage.