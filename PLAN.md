---
tags: [project/plan, kotlin/android, ai/pose-detection, hackathon]
aliases: [AIFit-Tracker-Plan]
date_created: {{date:YYYY-MM-DD}}
status: #draft
---

# Lập Kế Hoạch Dự Án: AI Fit Tracker (Proof of Concept)

> **Cốt lõi (TL;DR):** Ứng dụng Android đánh giá tư thế tập luyện (Workout Posture) theo thời gian thực sử dụng Camera điện thoại và công nghệ Pose Detection. Hoàn thành trong 3-4 ngày dưới dạng PoC (Proof of Concept).

## 1. 🎯 Mục tiêu & Phạm vi (Scope)
*   **Mục tiêu:** Xây dựng một luồng ứng dụng hoàn chỉnh: Mở camera -> Nhận diện cơ thể -> Đếm số rep (ví dụ: Squat) -> Cảnh báo form sai.
*   **Phạm vi (Giới hạn trong 3-4 ngày):**
    *   Chỉ hỗ trợ 1 bài tập duy nhất để chứng minh tính khả thi (Ví dụ: **Squat**).
    *   Không có tính năng đăng nhập, lưu trữ cloud phức tạp.
    *   Giao diện (UI) đơn giản, tập trung vào màn hình Camera và lớp phủ (Overlay) vẽ khung xương.

## 2. 🛠️ Kiến Trúc & Tech Stack
*   **Workflow IDE:** 
    *   **Viết Code logic:** VS Code (thông qua workspace `antigravity`).
    *   **UI Preview & Debugging:** Android Studio (cho Compose Preview và logcat từ thiết bị thật).
*   **Ngôn ngữ:** Kotlin.
*   **UI Framework:** Jetpack Compose (Tối ưu để vẽ các thành phần động như Overlay khung xương).
*   **Computer Vision / AI Engine:** **Google ML Kit Pose Detection** (Nhanh, nhẹ, chạy on-device, không cần internet, dễ tích hợp hơn TensorFlow Lite thuần).
*   **Camera API:** CameraX (Dễ dàng lấy luồng frames liên tục qua `ImageAnalysis`).
*   **Xử lý bất đồng bộ:** Coroutines & Flow (để chuyển luồng tọa độ từ ML Kit sang UI để vẽ).

## 3. 🚀 Lộ Trình Triển Khai (3-4 Ngày)

### 📌 Ngày 1: Setup Cơ Bản & Camera (Core Infrastructure)
*   Khởi tạo project Android Studio (Empty Compose Activity).
*   Thêm các dependencies cần thiết: CameraX, Jetpack Compose, ML Kit Pose Detection.
*   Xin quyền Camera.
*   Triển khai màn hình `CameraPreview` bằng CameraX và Jetpack Compose. Đảm bảo lấy được luồng hình ảnh ổn định.

### 📌 Ngày 2: Tích hợp AI (Pose Detection)
*   Triển khai `ImageAnalysis.Analyzer` để nhận các frame hình ảnh từ CameraX.
*   Đẩy frame vào ML Kit Pose Detection.
*   Trích xuất tọa độ của 33 điểm khớp (Landmarks) trên cơ thể.
*   Tạo một `Canvas` overlay (trong suốt) đè lên `CameraPreview`. Vẽ các đường nối (ví dụ: Vai -> Hông -> Đầu gối) dựa trên tọa độ lấy được.

### 📌 Ngày 3: Logic Toán Học & Đếm Rep (Squat)
*   **Toán học:** Viết hàm tính **Góc (Angle)** giữa 3 điểm (ví dụ: Góc tạo bởi Hông, Đầu gối, và Cổ chân).
*   **Logic Squat:**
    *   *Trạng thái Đứng:* Góc đầu gối > 160 độ.
    *   *Trạng thái Ngồi (Squat sâu):* Góc đầu gối < 90 độ (Form chuẩn). Nếu góc > 90 nhưng lại đứng lên -> Form sai (không tính rep).
*   Cập nhật UI: Hiển thị bộ đếm Rep (Rep Counter) to, rõ ràng. Đổi màu khung xương (Xanh = form đúng, Đỏ = form sai).

### 📌 Ngày 4: Hoàn Thiện & Tối Ưu (Polish)
*   Thêm phản hồi âm thanh/Rung (Text-to-Speech hoặc Vibrator) khi người dùng hoàn thành 1 rep đúng hoặc khi làm sai.
*   Xử lý các Edge Cases: Không có người trong khung hình, có nhiều người trong khung hình (ML Kit mặc định bắt người rõ nhất).
*   Test trên thiết bị thật và tối ưu hiệu năng (tránh bị giật lag khung hình).

## 4. 🔗 Hướng mở rộng tương lai (Nếu phát triển thành App thương mại)
*   Tích hợp Model tùy chỉnh (Custom TFLite model) để nhận diện nhiều bài tập phức tạp hơn (Push-up, Plank).
*   Lưu lịch sử tập luyện (Room Database).
*   Thiết kế giao diện Dashboard theo dõi tiến độ.
