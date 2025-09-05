# ContentPocket
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

No clickbaits. No feeds. No shorts. No distractions.

ContentPocket is a cross-platform Java desktop application designed for checking and watching latest chosen content in distraction-free mode.

**Subscriptions tab:** Add your trusted channels or content providers, fetch titles progressively from latest and decide what to play or keep as audio, video, or both (with per-channel format and quality preferences).

**Batch tab:** Keep meaningful or your favourite content safe. Paste links, choose all formats you need, define output structure, and control your system load with adjustable multithreading.

---

## 📥 Installation

### 🪟 Windows - "Plug & Play" - Portable ZIP (tools bundled) 
This distribution already includes the required executables (`ffmpeg`, `ffprobe`, `yt-dlp`).  
No setup needed:

1. Download the ZIP.
2. Extract files.
3. Run `ContentPocket.exe`.

That’s all.

---

### 🪟 Windows - Lightweight ZIP (tools not bundled)
If you already have `ffmpeg`, `ffprobe`, and `yt-dlp` installed (either in a folder or on your system/user PATH), you can use lightweight (without tools execs inside) distribution.

1. Download the ZIP.
2. Extract files.
3. Run `ContentPocket.exe`.

The app will:
- First check whether required binaries are available via `PATH`.
- If not found, App will ask you to provide a directory with tools execs.

**Requirement:** all tools executables must be placed in the same directory.

That's all.

---

### 🍏 macOS: DMG image (tools not bundled)
As Apple Inc. does not try to make developer's life easier (unless you pay 100$/y), 
required tools binaries could not be easily bundled, and installing this App on macOS needs some more hustle.

1. Install `ffmpeg`, `ffprobe`, and `yt-dlp_macos`. 
 
The app will look for them:
  - On your user/system `PATH`.
  - In `~/bin/`.
  - If not found, App will ask you to provide a directory with tools binaries.

**Requirement:** all tools must be placed in the same directory.

2. Make sure the binaries have required permissions:
   ```bash
   chmod 755 ffmpeg ffprobe yt-dlp_macos
   ```
- Or setup more restrictive custom permissions with at least: `yt-dlp_macos`: rwx, `ffmpeg`: rwx, `ffprobe`: rx

3. Allow the app from 'Unknown Developer' in macOS Security & Privacy settings:
[Apple Support Guide](https://support.apple.com/guide/mac-help/mh40616/mac)


4. If App still have problem  still fails, run the app from Terminal:
   ```bash
   /Applications/ContentPocket.app/Contents/MacOS/ContentPocket
   ```

Good luck.

---
### License
This project is licensed under the [MIT License](LICENSE).

You are free to use, modify, and distribute this software.  
If you do, please include the original copyright notice and mention me as the author.

### Third-party software

This application bundles prebuilt binaries of:

- [FFmpeg 7.1.1](https://ffmpeg.org/) and [FFprobe 7.1.1](https://ffmpeg.org/)  
  Licensed under the GNU GPL v3 (see `/licenses/GPL-3.0.txt` and `/licenses/LICENSE-FFmpeg.md`).

- [yt-dlp 2025.06.09](https://github.com/yt-dlp/yt-dlp)  
  Licensed under the Unlicense (see `/licenses/UNLICENSE.txt`).

On Windows release, these executables are bundled unmodified in `/tools`.  
On macOS release, the user must install them manually (tested with versions: FFmpeg 7.1.1, yt-dlp 2025.06.09).


### GitHub 'About' section --> move from readme to github ui but atm store here for reviewing
Cross-platform JavaFX desktop app for distraction-free browsing, watching, and keeping content - powered by yt-dlp & FFmpeg.