# Try-Again — Fabric 1.21.11

Mod server-side cho Minecraft 1.21.11 (Fabric). Hỗ trợ Java và Bedrock (Geyser).  
Tính năng chính hiện tại: xác thực tài khoản, home, teleport (tpa). Hệ thống đa ngôn ngữ tuỳ chỉnh.

---

## Yêu cầu

| Thứ | Phiên bản |
|-----|-----------|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.18.1 |
| Fabric API | 0.140.2+1.21.11 |
| Java | 21 |
| Geyser/Floodgate | Tùy chọn (Bedrock support) |

---

## Cài đặt

```
1. Build: ./gradlew build
2. Copy build/libs/try-again-1.0.0.jar (hoặc tên tương tự) vào thư mục mods/ của server
3. Khởi động server — thư mục config tự tạo tại config/tryagain/
4. Thêm token vào config/tryagain/token.json trước khi cho người chơi đăng ký
```

---

## Cấu trúc dự án

```
src/
├── main/java/com/minhduong/
│   ├── TryAgain.java                 Entry point — khởi tạo và đăng ký mọi thứ
│   │
│   ├── command/
│   │   ├── LoginCommand.java         /login <mat_khau>
│   │   ├── RegisterCommand.java      /register <pass> <nhac_lai> <token>
│   │   ├── HomeCommand.java          /home, /sethome, /delhome, /lshome
│   │   ├── TpaCommand.java           /tpa, /tpaccept, /tpdeny
│   │   ├── LanguageCommand.java      /language reload
│   │   └── HelpCommand.java          /tryagain
│   │
│   ├── data/
│   │   ├── PlayerDataManager.java    Quản lý tài khoản (players.json)
│   │   ├── HomeManager.java          Quản lý nhà (homes.json)
│   │   ├── TokenConfig.java          Quản lý các token (token.json)
│   │   └── LanguageManager.java      Nạp và lưu cache hệ thống đa ngôn ngữ
│   │
│   ├── events/
│   │   └── AuthEventHandler.java     Đóng băng người chơi, check timeout
│   │
│   └── util/
│       ├── Messages.java             Toàn bộ text in-game (hỗ trợ đa ngôn ngữ)
│       └── TextFormat.java           Format mã màu hex/legacy và component
│
└── main/resources/assets/tryagain/lang/
    ├── vi_vn.json                    Ngôn ngữ Tiếng Việt mặc định
    └── en_us.json                    Ngôn ngữ Tiếng Anh
```

---

## Config files (tự tạo lần đầu chạy)

```
config/tryagain/
├── token.json      Danh sách token đăng ký (admin thêm thủ công, tự xóa sau khi dùng)
├── players.json    Tài khoản người chơi {username: password}
├── homes.json      Danh sách home {username: {name: {worldId, x, y, z}}}
└── lang/           Các file JSON đa ngôn ngữ trích xuất từ mod
```

Ví dụ `token.json`:
```json
{
  "tokens": ["TOKEN_A", "TOKEN_B", "TOKEN_C"]
}
```

---

## Đa Ngôn Ngữ & Tuỳ Chỉnh Tin Nhắn (I18n & Formatting)

Mod hỗ trợ hệ thống đa ngôn ngữ hoàn chỉnh và cho phép trang trí tin nhắn (hỗ trợ mã màu Hex `<#RRGGBB>` và màu Legacy `&a`, `&l`...).

**Cách đóng góp ngôn ngữ mới trên GitHub:**
1. Fork dự án và truy cập vào thư mục `src/main/resources/assets/tryagain/lang/`.
2. Tạo hoặc sao chép file JSON có sẵn (ví dụ: `vi_vn.json`, `en_us.json`) sang một ngôn ngữ mới (ví dụ: `zh_cn.json`).
3. Commit và tạo Pull Request để đóng góp bản dịch của bạn!

**Ghi đè cấu hình tại server (Không cần đụng vào code):**
- Khi server chạy, các file ngôn ngữ mặc định sẽ được copy ra `config/tryagain/lang/`.
- Admin có thể mở các file `.json` tại đây để tuỳ chỉnh lại màu sắc, chữ viết theo ý muốn. File ở thư mục này sẽ ghi đè nội dung gốc của Mod.
- Dùng lệnh `/language reload` trong game để cập nhật text ngay lập tức mà không cần khởi động lại Server.

---

## Lệnh

### Xác thực

| Lệnh | Mô tả |
|------|-------|
| `/login <mat_khau>` | Đăng nhập. Bắt buộc sau mỗi lần join |
| `/register <pass> <nhac_lai> <token>` | Tạo tài khoản mới. Token do admin cấp, dùng một lần |
| `/tryagain` | Xem hướng dẫn hệ thống |
| `/language reload` | Refresh lại config ngôn ngữ (Yêu cầu quyền OP) |

> Người chơi bị đóng băng tại chỗ và bị kick nếu quá thời hạn chưa xác thực xong.

### Home

Hỗ trợ tối đa 5 home cho mỗi người chơi (có thể cross-dimension):

| Lệnh | Mô tả |
|------|-------|
| `/sethome <tên>` | Lưu vị trí hiện tại thành home mang tên đã chỉ định |
| `/home <tên>` | Dịch chuyển về home (Tab để xem các home hiện có) |
| `/delhome <tên>` | Xóa một home hiện có |
| `/lshome` | Liệt kê tất cả danh sách home của bạn |

### Teleport

| Lệnh | Mô tả |
|------|-------|
| `/tpa <ten>` | Gửi yêu cầu teleport đến một người chơi đang online |
| `/tpaccept` | Chấp nhận yêu cầu teleport |
| `/tpdeny` | Từ chối yêu cầu teleport |

---

## Bedrock (Geyser/Floodgate)

Người chơi Bedrock sử dụng lệnh hoàn toàn giống hệt Java — không có giao diện form đặc biệt. Tên Bedrock tự động được chuẩn hoá, lưu giữ phiên và không xung đột với tài khoản Java.

---

## Tính năng sắp tới (Scaffolds)
*(Các tính năng đang trong quá trình phát triển chưa được kích hoạt chính thức)*

- **Hệ thống kinh tế (`/balance`)**
- **Cửa hàng và thị trường giá động (`/buy`, `/sell`)**: Giá trị món đồ sẽ tuỳ thuộc vào biến động kho hàng trong hệ thống theo thời gian thực.
- **HUD Sidebar Scoreboard**: Thanh bên thông báo tình hình tài chính của người chơi.