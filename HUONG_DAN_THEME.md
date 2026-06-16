# Hướng Dẫn Cấu Hình Theme & Giao Diện AI Fit Tracker 🎨

Dự án đã được cấu hình hệ thống **Design System / Theme** đồng bộ theo phong cách **Cyberpunk / Neon Dark Mode** hiện đại.

Tài liệu này hướng dẫn cách tổ chức tệp theme và cách tùy biến màu sắc giao diện theo ý muốn của bạn.

---

## 📂 1. Cấu Trúc Các Tệp Theme

Hệ thống theme được đặt trong gói `com.example.aifittracker.ui.theme` gồm các tệp:
1.  **[Color.kt](file:///home/longnhat/Obsidian%20Vault/01%20Projects/AI-Fit-Tracker/app/src/main/java/com/example/aifittracker/ui/theme/Color.kt):** Nơi định nghĩa toàn bộ mã màu (Color Tokens) được sử dụng trong ứng dụng.
2.  **[Theme.kt](file:///home/longnhat/Obsidian%20Vault/01%20Projects/AI-Fit-Tracker/app/src/main/java/com/example/aifittracker/ui/theme/Theme.kt):** Thiết lập `ColorScheme` của Jetpack Compose và bọc ứng dụng để đồng bộ hóa giao diện.

---

## 🎨 2. Bảng Màu Cyberpunk Đang Sử Dụng

Bảng màu được thiết kế trên nền không gian sâu (Deep Space) kết hợp với các dải đèn Neon phát sáng để tôn lên cấu trúc khung xương AI:

| Token | Mã Màu | Minh Họa | Vai Trò Giao Diện |
| :--- | :--- | :--- | :--- |
| **`CyberDarkBg`** | `#0A0E17` | 🌌 Tối Sậm | Nền chủ đạo của toàn bộ các màn hình. |
| **`CyberSurfaceBg`**| `#151D2A` | ⬛ Xám Xanh | Màu nền của các thẻ Card, Bottom Bar, Dialog. |
| **`CyberPrimary`** | `#00E5FF` | 💎 Cyan Neon | Màu nhấn chính, nút bấm, trạng thái đang thực hiện. |
| **`CyberSecondary`**| `#00E676` | 🟢 Green Neon| Biểu tượng đếm Rep, tư thế chuẩn (Correct form). |
| **`CyberAccent`** | `#FFFF3D00`| 🔴 Red Neon  | Cảnh báo sai tư thế (Error form), nút Reset. |
| **`CyberGold`** | `#FFFFD54F`| 🟡 Gold      | Màu sắc đại diện cho tiền xu FitCoin. |

---

## 🔄 3. Cách Thay Đổi Sang Chủ Đề Khác (Ví dụ: Sunset/Hot Pink)

Nếu bạn muốn thay đổi toàn bộ tông màu của ứng dụng sang phong cách năng động khác (ví dụ: Hồng hoàng hôn Sunset Pink), bạn chỉ cần mở tệp [Color.kt](file:///home/longnhat/Obsidian%20Vault/01%20Projects/AI-Fit-Tracker/app/src/main/java/com/example/aifittracker/ui/theme/Color.kt) và thay đổi giá trị của các mã màu:

```kotlin
// Ví dụ: Đổi sang Sunset Pink Theme
val CyberPrimary = Color(0xFFFF4081)      // Đổi Cyan thành Neon Pink
val CyberSecondary = Color(0xFFFFAB40)    // Đổi Green thành Orange Gold
val CyberDarkBg = Color(0xFF120E16)       // Nền tối ánh tím sậm
val CyberSurfaceBg = Color(0xFF1D1824)    // Thẻ card tím xám sậm
```

Toàn bộ ứng dụng (bao gồm các nút bấm, khung xương vẽ trên camera, biểu tượng trạng thái và thẻ sản phẩm) sẽ tự động đồng bộ theo bảng màu mới mà không cần sửa bất kỳ dòng logic nào!

---

## 🚀 Cách Thuyết Trình Về Theme Trong Đồ Án Cuối Kỳ

Khi bảo vệ đồ án trước hội đồng giảng viên, bạn có thể nhấn mạnh:
> *"Em sử dụng cơ chế Jetpack Compose Theme để quản lý tập trung toàn bộ hệ thống màu sắc (Design Tokens). Điều này giúp ứng dụng dễ dàng mở rộng sang tính năng đổi Theme động (Dynamic Theming) hoặc hỗ trợ cả Light/Dark Mode một cách linh hoạt bằng cách thay đổi ColorScheme mà không làm ảnh hưởng đến mã nguồn logic."*
