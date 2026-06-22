MILESTONES.md

Detailed Milestone Specifications

Milestone 2 - iOS Support

Goal

Implement iOS barcode scanning while preserving Android API compatibility.

Technology

Camera:

* AVFoundation

Barcode Detection:

* Vision Framework

Requirements

Support:

* CODE_128

Must support:

* Shared ScanResult
* Shared onScan callback
* Shared ScannerView

Acceptance Criteria

* ScannerView renders camera preview on iOS
* CODE_128 scanning works
* ScanResult matches Android structure
* No platform-specific props required

⸻

Milestone 3 - Production Hardening

Goal

Make scanner production ready.

Features

Torch Support

<ScannerView torch />

Duplicate Filtering

<ScannerView duplicateTimeout={750} />

Disabled by default.

Lifecycle Handling

Handle:

* App backgrounding
* App foregrounding
* Camera interruptions

Error Handling

Handle:

* Permission denied
* Camera unavailable
* Decoder failures

Metrics

Internal only.

Track:

* Camera startup time
* Decode latency
* Scan count

Acceptance Criteria

* No crashes during repeated scanning
* Torch works on both platforms
* Duplicate filtering behaves consistently

⸻

Milestone 4 - Performance Optimization

Goal

Optimize for high-volume scanning.

Features

Process Latest Frame

Do not queue frames.

If analyzer is busy:

Drop incoming frame.

ROI Support

roi={{
  x: 0.25,
  y: 0.25,
  width: 0.5,
  height: 0.5
}}

Benchmarking

Measure:

* Startup time
* Decode latency
* CPU usage
* Memory usage

Device Coverage

Test:

* High-end Android
* Mid-range Android
* Older Android devices
* Older iPhones

Acceptance Criteria

* Scanning remains responsive
* CPU usage reduced compared to full-frame processing
* No frame backlog

⸻

Milestone 5 - Provider Architecture

Goal

Prepare for enterprise scanner support.

New Components

ScannerProvider
ScannerManager

Providers

Initially:

CameraScannerProvider

only.

Requirements

No public API changes.

Acceptance Criteria

Application code remains unchanged.

⸻

Milestone 6 - Zebra Support

Goal

Support Zebra enterprise scanners.

Integration

Use:

* DataWedge

Provider

ZebraScannerProvider

Requirements

Produce same ScanResult structure.

Acceptance Criteria

Applications receive scans exactly as camera scans.

⸻

Milestone 7 - Honeywell Support

Goal

Support Honeywell enterprise scanners.

Provider

HoneywellScannerProvider

Requirements

Produce same ScanResult structure.

Acceptance Criteria

Applications receive scans exactly as camera scans.

⸻

Milestone 8 - Additional Barcode Formats

Goal

Expand barcode support safely.

Candidate Formats

* QR
* EAN13
* CODE39

Requirements

Format detection must remain configurable.

Example:

formats={['code128']}

must continue to restrict decoder work.

Acceptance Criteria

Performance regression is minimal when additional formats are enabled.