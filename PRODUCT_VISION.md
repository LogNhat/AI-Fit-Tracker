# Tầm Nhìn Sản Phẩm: Hệ Sinh Thái AI Fit Tracker (Tầm Nhìn Triển Khai Lớn)

Tài liệu này phát triển ý tưởng ban đầu từ [Ý tưởng.md](file:///home/longnhat/Obsidian%20Vault/01%20Projects/AI-Fit-Tracker/app/%C3%9D%20t%C6%B0%E1%BB%9Fng.md) thành một hệ sinh thái Siêu ứng dụng Thể thao & Xã hội (Social Fitness Super-App) sử dụng AI và mạng xã hội.

---

## 🚀 1. Tầm Nhìn Chiến Lược & Các Trụ Cột Tính Năng

```
                      ┌─────────────────────────────────────────┐
                      │      AI FIT TRACKER SUPER-APP           │
                      └────────────────────┬────────────────────┘
                                           │
         ┌───────────────────┬─────────────┴─────────────┬───────────────────┐
         ▼                   ▼                           ▼                   ▼
┌─────────────────┐ ┌─────────────────┐         ┌─────────────────┐ ┌─────────────────┐
│  AI Multi-Pose  │ │ Virtual Rooms   │         │  PT Marketplace │ │ FitTok & Shop   │
│  (Đa bài tập)   │ │ (Phòng tập ảo)  │         │  (Kết nối PT)   │ │ (Mạng XH & CH)  │
└─────────────────┘ └─────────────────┘         └─────────────────┘ └─────────────────┘
```

### 🏋️ Trụ cột 1: Động Cơ AI Đa Bài Tập (AI Multi-Pose Engine)
Phát triển từ đếm Squat đơn lẻ thành bộ thư viện phân tích tư thế đa dạng bài tập (Cardio, Strength, Flexibility):
- **Push-ups (Hít đất):** Đánh giá góc khuỷu tay (độ sâu) và độ thẳng của lưng (tránh võng lưng).
- **Plank:** Nhận diện vị trí Hông so với Vai và Cổ chân để đảm bảo người tập giữ thẳng người, cảnh báo nếu hông quá cao hoặc võng thấp.
- **Jumping Jacks:** Đếm nhịp nhảy dựa trên độ mở rộng của tay và chân.
- **Lunge:** Đo góc vuông đầu gối để bảo vệ khớp.
- **Kiến trúc Modular:** Xây dựng Interface `ExerciseAnalyzer` để dễ dàng định nghĩa bài tập mới bằng toán học hoặc tích hợp mô hình TensorFlow Lite tùy chỉnh trong tương lai.

### 👥 Trụ cột 2: Phòng Tập Ảo Realtime (Virtual Workout Rooms)
Thay vì tập một mình, người dùng có thể tạo phòng tập nhóm kết nối từ 2 đến 10 người:
- **Tích hợp WebRTC:** Truyền luồng video camera và overlay khung xương thời gian thực giữa các thành viên.
- **Màn hình so sánh Side-by-Side:** Xem tư thế của bạn đặt cạnh tư thế của bạn bè hoặc huấn luyện viên mẫu để tự điều chỉnh.
- **Voice Chat & Emotion Effects:** Trò chuyện trực tiếp bằng giọng nói, thả các sticker cổ vũ khi bạn cùng phòng đạt mốc 10 reps, 20 reps.

### 🎓 Trụ cột 3: Sàn Giao Dịch PT & Trainee (PT-Trainee Marketplace)
Hệ thống kết nối huấn luyện viên cá nhân (Personal Trainer) và người tập:
- **Giám sát từ xa (Remote Coaching):** PT có thể theo dõi trực tiếp buổi tập của học viên qua luồng skeleton (chỉ truyền khung xương AI để bảo mật hình ảnh cá nhân và tiết kiệm băng thông).
- **Dashboard quản lý học viên:** PT nhận báo cáo thống kê tự động về chất lượng form tập (ví dụ: "Học viên A tập Squat bị võng đầu gối 30% tổng thời gian").
- **Thiết lập giáo án:** PT tạo và gửi lịch trình tập luyện riêng biệt trực tiếp vào app của học viên.

### 🛍️ Trụ cột 4: FitStore & Nền Kinh Tế FitCoin (E-Commerce & Gamification)
Biến hoạt động thể chất thành phần thưởng kinh tế:
- **FitCoin (Move-to-Earn):** Người tập nhận được FitCoin khi hoàn thành các mục tiêu hàng ngày hoặc thực hiện các rep tập chuẩn form (AI đánh giá độ chuẩn).
- **FitStore:** Cửa hàng tích hợp bán đồ tập, bình nước thông minh, thực phẩm bổ sung (Whey, Vitamin). Người dùng có thể dùng FitCoin tích lũy để đổi voucher giảm giá hoặc thanh toán một phần đơn hàng.

### 📱 Trụ cột 5: FitTok (Mạng Xã Hội Video Ngắn)
Kênh chia sẻ video ngắn tập trung vào thể hình:
- **AI Overlay Video:** Khi đăng video tập luyện lên FitTok, app hỗ trợ tự động vẽ và đè khung xương neon chuyển động lên cơ thể người tập để họ "khoe" tư thế chuẩn.
- **Thử thách Hashtag:** Tạo các trào lưu thử thách (ví dụ: `#SquatChallenge30s`) kết hợp bảng xếp hạng tự động của thử thách đó.

---

## 🛠️ 2. Kiến Trúc Hệ Thống Để Hiện Thực Hóa Ý Tưởng

Để hệ thống này vận hành mượt mà với quy mô lớn, kiến trúc backend cần được phân rã thành các Microservices:

```
[Android App] ──(WebRTC)──► [Live Video Room Service (Mediasoup / LiveKit)]
      │
(gRPC / HTTP)
      ▼
[API Gateway] ────────────┬──► [User & PT Auth Service]
                          ├──► [Workout Log & Sync Service]
                          ├──► [Gamification & FitCoin Wallet]
                          ├──► [FitStore E-Commerce (PostgreSQL)]
                          └──► [Social Feed & Video Feed (FitTok)]
```

---

## 🎯 3. Kế Hoạch Triển Khai Giai Đoạn 1 (Thực Thi Ngay)

Để bắt đầu hiện thực hóa ý tưởng lớn này ngay lập tức trên mã nguồn hiện tại, chúng ta sẽ thực hiện nâng cấp **Trụ cột 1** trước tiên:
1. **Tái cấu trúc bộ phân tích AI (`PoseAnalyzer`)** thành dạng thiết kế hướng đối tượng có thể mở rộng (chọn nhiều bài tập).
2. **Thêm logic phân tích cho 2 bài tập mới**:
   - **Push-ups (Hít đất)**: Đo góc khớp khuỷu tay (độ sâu) để đếm reps.
   - **Planks**: Đo độ thẳng hàng của Vai - Hông - Cổ chân để kiểm tra giữ form chuẩn.
3. **Xây dựng Màn hình Chọn Bài Tập (Exercise Selection Screen)**:
   - Cho phép người dùng vuốt chọn giữa: Squat, Push-up, và Plank trước khi mở camera.
   - Hiển thị hướng dẫn nhanh (ảnh minh họa/text) cho mỗi bài tập.
