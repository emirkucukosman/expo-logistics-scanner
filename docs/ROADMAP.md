ROADMAP.md

Expo Logistics Scanner Roadmap

Project Vision

Expo Logistics Scanner is a high-performance barcode scanning library built specifically for logistics and delivery workflows.

The library must prioritize:

* Fast barcode scanning
* Low-end device compatibility
* Native performance
* Expo compatibility
* Cross-platform consistency
* Enterprise extensibility

The library is intentionally focused on barcode scanning and should avoid becoming a generic camera framework.

Primary User Workflow

Delivery employees scan:

* 150-200 packages during morning loading
* Individual packages during delivery
* Large numbers of CODE_128 barcodes

Scanning must remain reliable on:

* Android phones
* iPhones
* Future enterprise scanner devices

Architecture Principles

Native First

Barcode decoding must always occur natively.

Preferred flow:

Camera
→ Native Decoder
→ Scan Result
→ JS Event

Never:

Camera
→ JS
→ Decode

Provider-Based Future

Applications should not care where scans originate.

Future providers:

* Camera Scanner
* Zebra Scanner
* Honeywell Scanner

All providers must eventually produce identical scan events.

Minimal API Surface

Only expose functionality that provides direct value to logistics workflows.

Avoid:

* OCR
* Face detection
* Video recording
* Document scanning
* Generic camera features

Current Status

Completed:

* Expo Module setup
* Android implementation
* CameraX integration
* ML Kit integration
* CODE_128 scanning
* React Native ScannerView
* Scan event emission

Roadmap

Milestone 1 (Completed)

Android MVP

Deliverables:

* Android only
* CameraX Preview
* ML Kit barcode scanning
* CODE_128 support
* ScannerView component
* Scan events

Status:

Completed

Milestone 2

iOS Support

Goal:

Validate cross-platform architecture and API consistency.

Deliverables:

* AVFoundation integration
* Vision Framework integration
* CODE_128 support
* Shared API compatibility
* Shared ScanResult structure
* Shared torch support

Must not introduce platform-specific APIs.

Milestone 3

Production Hardening

Goal:

Make library suitable for production use.

Deliverables:

* Torch support
* Duplicate filtering
* Lifecycle handling
* Error handling
* Background/foreground support
* Internal metrics collection

Milestone 4

Performance Optimization

Goal:

Optimize scanning for high-volume logistics workflows.

Deliverables:

* ROI support
* Process-latest-frame strategy
* Performance benchmarking
* Startup time metrics
* Decode latency metrics
* CPU and memory analysis

Milestone 5

Provider Architecture

Goal:

Prepare architecture for enterprise scanner support.

Deliverables:

* ScannerProvider abstraction
* ScannerManager
* CameraScannerProvider

No functionality changes expected.

Milestone 6

Zebra Support

Goal:

Support Zebra enterprise scanners.

Deliverables:

* DataWedge integration
* Zebra scanner provider
* Shared scan events

Milestone 7

Honeywell Support

Goal:

Support Honeywell enterprise scanners.

Deliverables:

* Honeywell scanner provider
* Shared scan events

Milestone 8

Additional Barcode Formats

Goal:

Expand supported formats without compromising performance.

Potential formats:

* QR
* EAN13
* CODE39

Must remain configurable and performance-focused.