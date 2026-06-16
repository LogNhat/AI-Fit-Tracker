# Đánh Giá Đồ Án Cuối Kỳ: AI Fit Tracker 🎓

Tài liệu này đánh giá tính khả thi, quy mô công việc của dự án **AI Fit Tracker** đối với đồ án cuối kỳ thực hiện bởi **1 người**, đồng thời đề xuất các cải tiến để đạt điểm tối đa (A+).

---

## 🌟 1. Dự Án Như Thế Này Đã Đủ Cho Đồ Án 1 Người Chưa?

**Trả lời ngắn gọn:** **HƠN CẢ ĐỦ!** Dự án này có độ khó và quy mô vượt trội so với mặt bằng chung đồ án cuối kỳ của sinh viên CNTT (thường là các app CRUD quản lý, bán hàng đơn giản).

### Các điểm nhấn kỹ thuật cực kỳ đắt giá (Giảng viên sẽ đánh giá rất cao):
1.  **Tích hợp Trí Tuệ Nhân Tạo (AI on-device):** Sử dụng Google ML Kit để phân tích 33 tọa độ khớp xương thời gian thực trực tiếp trên điện thoại. Đây là điểm cộng lớn về mặt công nghệ (Computer Vision).
2.  **Xử lý phần cứng và đa phương tiện:** Làm việc trực tiếp với CameraX (lấy luồng hình ảnh liên tục), Text-to-Speech (phát âm thanh phản hồi), và Hệ thống Rung (Vibrator) báo lỗi/thành công.
3.  **Thuật toán & Toán học ứng dụng:** Sử dụng toán vectơ lượng giác (hàm lượng giác `atan2`) để tính góc quay của đầu gối, khuỷu tay, hông nhằm đưa ra phản hồi động.
4.  **Máy trạng thái hữu hạn (FSM - Finite State Machine):** Xử lý luồng động tác của người dùng qua các trạng thái (ví dụ: STANDING -> DESCENDING -> BOTTOM -> ASCENDING) để đếm reps chính xác, lọc bỏ reps lỗi.
5.  **Giao diện UI/UX Hiện Đại:** Sử dụng Jetpack Compose vẽ Canvas động đè lên camera (Skeleton Overlay), giao diện tối màu (Dark Mode) kết hợp phong cách Neon hiện đại.
6.  **Tầm nhìn sản phẩm hoàn chỉnh:** Có cả hệ sinh thái xung quanh (Ví xu Move-to-Earn, FitStore, Multiplayer Rooms, FitTok Feed, PT Chat, Leaderboard) tạo cảm giác như một sản phẩm khởi nghiệp thực tế.

---

## 🛠️ 2. Có Thể Phát Triển Thêm Phần Nào Để Lấy Điểm Tuyệt Đối?

Để đồ án trở nên **hoàn hảo không tỳ vết** và thuyết phục những giảng viên khó tính nhất, bạn nên bổ sung thêm các phần sau:

### 💾 A. Cơ chế Lưu trữ Dữ liệu Thực tế (Data Persistence)
*   **Hiện trạng:** Số dư FitCoins và số reps tập đang được lưu tạm trên RAM (`remember`). Khi tắt app mở lại, dữ liệu sẽ quay về mặc định (150 xu, 0 reps).
*   **Giải pháp nâng cấp:** Dùng `SharedPreferences` (hoặc Jetpack DataStore) để lưu lại số xu tích lũy và kỷ lục tập luyện (High Score) của người dùng. Khi mở lại app, dữ liệu cũ vẫn được giữ nguyên. Điều này chứng minh bạn biết xử lý dữ liệu bền vững.

### 📝 B. Báo cáo kỹ thuật chi tiết (Technical Report)
*   **Giải pháp:** Viết một chương mô tả chi tiết cách ML Kit hoạt động, các góc xương được tính toán bằng công thức toán học nào, và sơ đồ máy trạng thái đếm reps. Phần này dùng để đưa thẳng vào báo cáo word/slide thuyết trình.

### 🧪 C. Kiểm thử Đơn vị (Unit Tests)
*   **Giải pháp:** Viết một vài đoạn Unit Test để kiểm tra logic tính góc xương trong `PoseAnalyzer.kt` và chuyển đổi trạng thái của `SquatState`. Phần này chứng minh dự án có chất lượng kiểm thử tốt (Software Quality Assurance).

---

## 🚀 Đề xuất hành động tiếp theo

Tôi khuyến nghị chúng ta nên triển khai **Phần A (Lưu trữ dữ liệu thực tế bằng SharedPreferences)** ngay bây giờ. Nó rất nhanh (khoảng 10-15 phút viết code) nhưng sẽ nâng tầm đồ án từ "mô phỏng" thành "ứng dụng chạy thực tế hoàn chỉnh".

Bạn có muốn tôi tiến hành tích hợp `SharedPreferences` để lưu xu và rep tập không?


