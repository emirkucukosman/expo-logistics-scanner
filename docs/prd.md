Expo Logistics Scanner

Purpose

Expo Logistics Scanner is a high-performance barcode scanning library for React Native and Expo applications used in logistics, delivery, warehouse, and field operations.

The primary use case is rapid package scanning by delivery employees who may scan hundreds of barcodes per day. Existing barcode scanning libraries are either too generic, contain unnecessary features, or are not optimized for low-end Android devices commonly used in enterprise environments.

This library prioritizes:

* Fast barcode detection
* Low CPU and memory usage
* Low-end Android device compatibility
* Native-first architecture
* Expo compatibility through Expo Modules
* Support for multiple scanner providers
* Consistent JavaScript API regardless of scan source

Non-Goals

The library is NOT intended to support:

* Face detection
* OCR / text recognition
* Document scanning
* Video recording
* Photo capture
* Augmented reality
* Generic camera functionality

The library should remain focused on barcode scanning.

Long-Term Vision

The library should support multiple scanning providers:

* Camera Scanner
* Zebra Scanner
* Honeywell Scanner
* Future enterprise scanner integrations

Applications should not need to know which provider generated the scan.

Example:

onScan(result)

should behave identically whether the scan originated from:

* Device camera
* Zebra hardware scanner
* Honeywell hardware scanner

Architecture Principles

Native-First

Barcode decoding must occur natively.

Avoid:

Camera -> JS -> Decode

Prefer:

Camera -> Native Decode -> JS Event

Provider-Based Design

Introduce a scanner provider abstraction.

Example:

interface ScannerProvider {
  start(): void
  stop(): void
}

Future providers:

* CameraScannerProvider
* ZebraScannerProvider
* HoneywellScannerProvider

Minimal Public API

Expose only the functionality required for barcode scanning.

Avoid adding optional features unless they improve logistics workflows.

Performance First

Design decisions should prioritize:

* Scan speed
* Startup time
* Battery efficiency
* CPU efficiency
* Stability

over feature count.

Supported Platforms

Initial MVP:

* Android

Future:

* Android
* iOS

Barcode Formats

Initial MVP:

* CODE_128

Future:

* QR
* EAN13
* CODE39
* Additional formats as needed

CODE_128 should be treated as the primary logistics format.

Technology Choices

Android

* CameraX
* ML Kit Barcode Scanning

iOS (future)

* AVFoundation
* Vision Framework

React Native

* Expo Modules API
* New Architecture compatible

Event Flow

Camera Frame
→ Native Decoder
→ Scan Result
→ Event Emitter
→ JavaScript

No image processing should occur on the JavaScript thread.

Success Criteria

The library should be capable of:

* Rapid consecutive scans
* Reliable operation on low-end Android devices
* Continuous scanning workflows
* Enterprise logistics environments

while maintaining a simple API.