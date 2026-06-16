# Hướng Dẫn Đóng Gói & Phát Hành Lên CH Play (Google Play Store)

Tài liệu này hướng dẫn chi tiết từng bước tạo mã ký ứng dụng (Keystore), cấu hình Gradle bảo mật, đóng gói định dạng `.aab` (Android App Bundle), và các thủ tục cần thiết trên Google Play Console.

---

## 🔑 Bước 1: Tạo Keystore Ký Ứng Dụng (Release Key)

Google Play yêu cầu tất cả các ứng dụng bản Production phải được ký bằng khóa bảo mật. Bạn cần chạy lệnh sau trong terminal để tạo khóa:

```bash
keytool -genkey -v -keystore release-key.keystore -alias fit-tracker-alias -keyalg RSA -keysize 2048 -validity 10000
```

> [!CAUTION]
> **CỰC KỲ QUAN TRỌNG:**
> - Lưu trữ tệp Keystore này ở nơi an toàn (ví dụ: Google Drive cá nhân, ổ cứng ngoài). Nếu làm mất khóa này, bạn **sẽ không thể cập nhật ứng dụng** trên CH Play được nữa.
> - **Tuyệt đối không push tệp keystore này lên GitHub công khai.**

---

## 🔒 Bước 2: Cấu Hình Ký Ứng Dụng Bảo Mật Trong Gradle

Để bảo mật mật khẩu Keystore, chúng ta nên tạo một tệp cục bộ có tên `keystore.properties` tại thư mục gốc của dự án (và thêm tệp này vào `.gitignore` để không bị lộ lên Git).

### 1. Tạo tệp `keystore.properties` tại thư mục gốc:
```properties
storeFile=../release-key.keystore
storePassword=mat_khau_cua_ban
keyAlias=fit-tracker-alias
keyPassword=mat_khau_cua_ban
```

### 2. Thêm vào `.gitignore`:
```text
# Keystore & Passwords
keystore.properties
*.keystore
*.jks
```

### 3. Cấu hình [app/build.gradle.kts](file:///home/longnhat/Obsidian%20Vault/01%20Projects/AI-Fit-Tracker/app/build.gradle.kts) để tự động đọc thông tin cấu hình ký:

```kotlin
import java.io.Properties
import java.io.FileInputStream

// Đọc keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    ...
    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Bật R8 để tối ưu hóa dung lượng & obfuscate code
            isShrinkResources = true // Loại bỏ tài nguyên dư thừa
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## 📦 Bước 3: Đóng Gói Định Dạng `.aab` (Android App Bundle)

Sử dụng Android App Bundle (`.aab`) thay vì `.apk` vì Google Play sẽ tự động chia nhỏ ứng dụng để người dùng tải xuống phiên bản nhẹ nhất khớp với cấu hình thiết bị của họ.

Chạy lệnh sau tại thư mục gốc dự án:
```bash
./gradlew bundleRelease
```
Tệp đầu ra sau khi build thành công sẽ nằm ở đường dẫn:
`app/build/outputs/bundle/release/app-release.aab`

---

## 🌐 Bước 4: Thiết Lập Trên Google Play Console

### 1. Đăng ký tài khoản nhà phát triển (Developer Account)
- Truy cập [Google Play Console](https://play.google.com/console).
- Đăng nhập bằng tài khoản Google của bạn và đóng phí đăng ký **$25 USD** (phí một lần duy nhất).

### 2. Tạo ứng dụng mới (Create App)
- Nhấn **Create app**.
- Điền các thông tin cơ bản: Tên ứng dụng (AI Fit Tracker), Ngôn ngữ mặc định (Tiếng Việt), Loại ứng dụng (App), và chọn Free.

### 3. Hoàn thiện các Tuyên bố Chính sách (App Content)
Google yêu cầu khai báo đầy đủ thông tin pháp lý/bảo mật. Hãy chuẩn bị các nội dung sau:
- **Chính sách Quyền riêng tư (Privacy Policy):** Bạn cần có 1 link web chứa chính sách bảo mật dữ liệu. 
  > *Gợi ý:* Vì AI Fit Tracker xử lý pose hoàn toàn on-device và không gửi hình ảnh/video về server, bạn có thể ghi rõ: *"Ứng dụng xử lý hình ảnh trực tiếp trên thiết bị của người dùng, cam kết không thu thập hay lưu trữ dữ liệu video/hình ảnh cá nhân của người sử dụng."*
- **An toàn Dữ liệu (Data Safety):** Khai báo rằng ứng dụng của bạn không chia sẻ dữ liệu với bên thứ ba.
- **Quyền truy cập (App Access):** Khai báo các tính năng cần quyền đặc biệt (ở đây là Camera).

---

## 🎨 Bước 5: Chuẩn Bị Tài Nguyên Cửa Hàng (Store Listing)

Chuẩn bị các tài nguyên đồ họa để đưa lên CH Play:
- **App Icon:** Kích thước `512x512 px` (định dạng PNG 32-bit).
- **Feature Graphic:** Ảnh bìa kích thước `1024x500 px` (định dạng JPG hoặc PNG).
- **Screenshots:** Ít nhất 2 ảnh chụp màn hình điện thoại thực tế khi đang sử dụng app.

---

## 🧪 Bước 6: Phát Hành Thử Nghiệm (Testing Tracks)

Trước khi phát hành rộng rãi cho tất cả mọi người, Google yêu cầu thử nghiệm:
1. **Kiểm thử nội bộ (Internal Testing):** Cho phép tối đa 100 người test tải ngay lập tức sau khi tải file `.aab` lên (không cần Google duyệt chính sách).
2. **Kiểm thử đóng (Closed Testing):** Cần tối thiểu **20 người dùng thử nghiệm liên tục trong 14 ngày** (đây là quy định mới bắt buộc từ năm 2024 đối với tài khoản cá nhân mới). Sau khi vượt qua vòng này, ứng dụng mới được phát hành chính thức lên CH Play.
