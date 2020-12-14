# Aion Desktop Wallet

Manage your AION coins using a local desktop application.



The Aion Desktop Wallet is a local application that allows you to create an Aion address and receive coins. The wallet is available for Linux, Mac, and Windows.

## Installation

Installation instructions and user guides can be found on the [Aion Docs website](https://docs-aion.theoan.com/docs/aion-desktop-wallet).

### Requirements

- Linux: `Ubuntu 16.04 LTS 64-bit` and higher
- Mac: `Mac OS High Sierra` and higher
- Windows: `Windows 10 64-bit`

## Features

- Generate a wallet specific to Aion (`m/44'/425'/0'/0'`).
- Create, manage, and export accounts.
- Import and export keystore accounts.
- Send and receive transactions.
- View transaction history.
- View the sync status of the node.

## Changelog

### v1.2.3

-The wallet was reskinned to align with the new OAN brand. No functional changes are included.

### v1.2.0

- Stripped a lot of bloat out of the application. Things run a lot faster now.
- The Ledger HID Driver has been rewritten. This allows us to directly interact with the JVM.
- Removed external dependencies related to the JVM.
- We've done a light rework on the wallet UI. This should clear up some of the confusion users had over certain tabs.
- Included a _Send All_ feature.
- Updating between wallet versions is now much smoother.
- Fixed a bunch of bugs submitted in [Github issues](https://github.com/aionnetwork/Desktop-Wallet/issues?utf8=%E2%9C%93&q=is%3Aissue+is%3Aclosed+).

### v1.1.0

- Added Mac and Windows support.

### v1.0.0

- Inital release.
