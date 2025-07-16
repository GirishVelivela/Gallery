# Gallery
Gallery

1. Custom Android Gallery App

This is a Custom Gallery App built using Kotlin and Jetpack Compose, designed to display all media (images & videos) on a user's device grouped by folder/album view.
It includes special albums like "All Photos", "All Videos", and "Camera".

---

## Features

- Shows media grouped by folder names
- Displays All Photos and All Videos from the device, excluding cache or system folders
- Shows a Camera album with all pictures/videos from the `DCIM/Camera` directory
- Displays number of media files in each album
- Clicking an album shows full-screen grid view of media files in that folder
- Smooth UI using Jetpack Compose with lazy grids and Coil image loading

- commits
- Data Layer creation
- Data Synch from ADO provider to local DB - WorkerManager
- Album  Screen changes
- Album media details screen
- Media details screen - final

---
 Tech Stack
- Kotlin
- Jetpack Compose
- MediaStore API
- Coil for image loading
- MVVM architecture
- Navigation Compose
- WorkerManager
