# AuthMod — Fabric 1.21.11

Mod server-side cho Minecraft 1.21.11 (Fabric). Hỗ trợ Java và Bedrock (Geyser).  
Tính năng: xác thực tài khoản, home, teleport, kinh tế, thị trường.

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
2. Copy build/libs/authmod-1.0.0.jar vào thư mục mods/ của server
3. Khởi động server — file config tự tạo tại config/authmod/
4. Thêm token vào config/authmod/config.json trước khi cho người chơi đăng ký
```

---

## Cấu trúc dự án

```
src/
├── main/java/com/authmod/
│   ├── AuthMod.java                  Entry point — khởi tạo và đăng ký mọi thứ
│   │
│   ├── commands/
│   │   ├── LoginCommand.java         /login <mat_khau>
│   │   ├── RegisterCommand.java      /register <pass> <nhac_lai> <token>
│   │   ├── HomeCommand.java          /home  /sethome  /delhome
│   │   ├── TpaCommand.java           /tpa  /tpaccept  /tpdeny
│   │   ├── SellCommand.java          /sell [so_luong]
│   │   ├── BuyCommand.java           /buy <item> [so_luong]  /buy list
│   │   ├── BalanceCommand.java       /balance
│   │   └── HelpCommand.java          /tryagain
│   │
│   ├── data/
│   │   ├── PlayerDataManager.java    Quản lý tài khoản (Fabric runtime)
│   │   ├── AccountStore.java         Logic tài khoản thuần Java (dùng cho test)
│   │   ├── TokenStore.java           Logic token thuần Java (dùng cho test)
│   │   ├── HomeData.java             Data class: worldId + x/y/z + yaw/pitch
│   │   ├── HomeManager.java          Lưu/tải home từ homes.json
│   │   ├── EconomyManager.java       Số dư xu + giá TP theo ngày
│   │   ├── TpaManager.java           Yêu cầu TP đang chờ (in-memory, hết hạn 30s)
│   │   └── MarketManager.java        Giá động + kho server
│   │
│   ├── events/
│   │   └── AuthEventHandler.java     Xử lý join/leave + tick handler
│   │
│   └── util/
│       ├── HudManager.java           Scoreboard sidebar HUD
│       ├── Messages.java             Tất cả tin nhắn gửi cho người chơi
│       ├── SessionManager.java       Theo dõi phiên chưa xác thực
│       └── TokenConfig.java          Quản lý token đăng ký (Fabric runtime)
│
└── test/java/com/authmod/
    ├── AccountStoreTest.java         13 test — đăng ký, đăng nhập, session, persistence
    ├── TokenStoreTest.java           5 test  — token dùng một lần
    ├── EconomyTest.java              7 test  — công thức giá TP nhân đôi
    ├── MarketPricingTest.java        6 test  — giá động theo kho
    └── TpaManagerTest.java           6 test  — yêu cầu teleport
```

---

## Config files (tự tạo lần đầu chạy)

```
config/authmod/
├── config.json     Token đăng ký (admin thêm thủ công, tự xóa sau khi dùng)
├── players.json    Tài khoản người chơi {username: password}
├── homes.json      Home của người chơi {username: {worldId, x, y, z, yaw, pitch}}
├── economy.json    Số dư xu + số lần TP trong ngày
└── market.json     Kho hàng server {stock: {item_id: count}}
```

Ví dụ `config.json`:
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
- Khi server chạy, các file ngôn ngữ mặc định sẽ được xuất ra `config/tryagain/lang/`.
- Admin có thể mở các file `.json` tại đây để tuỳ chỉnh lại màu sắc, chữ viết theo ý muốn. File config sẽ ưu tiên được đọc trước.
- Dùng lệnh `/tryagain reload` trong game hoặc console để cập nhật text ngay lập tức mà không cần khởi động lại Server.

---

## Lệnh

### Xác thực

| Lệnh | Mô tả |
|------|-------|
| `/login <mat_khau>` | Đăng nhập. Bắt buộc sau mỗi lần join |
| `/register <pass> <nhac_lai> <token>` | Tạo tài khoản mới. Token do admin cấp, dùng một lần |
| `/tryagain` | Xem hướng dẫn toàn bộ lệnh |

> Người chơi bị đóng băng tại chỗ và bị kick sau **60 giây** nếu chưa đăng nhập.

### Home

| Lệnh | Mô tả |
|------|-------|
| `/sethome` | Lưu vị trí hiện tại làm home |
| `/home` | Teleport về home. Hỗ trợ cross-dimension |
| `/delhome` | Xóa home |

### Teleport

| Lệnh | Mô tả |
|------|-------|
| `/tpa <ten>` | Gửi yêu cầu teleport đến người chơi khác. Tab để gợi ý tên |
| `/tpaccept` | Chấp nhận yêu cầu |
| `/tpdeny` | Từ chối yêu cầu |

**Giá teleport** (trừ từ người gửi `/tpa`):

| Lần trong ngày | Chi phí |
|---------------|---------|
| Lần 1 | 10 xu |
| Lần 2 | 20 xu |
| Lần 3 | 40 xu |
| Lần N | 10 × 2^(N-1) xu |

Reset về lần 1 lúc 0:00 mỗi ngày.

### Kinh tế

| Lệnh | Mô tả |
|------|-------|
| `/balance` | Xem số dư xu và thông tin TP hôm nay |
| `/sell` | Bán cả stack đang cầm tay phải |
| `/sell <so_luong>` | Bán một phần stack |
| `/buy <item>` | Mua 1 cái item từ server (Tab để gợi ý) |
| `/buy <item> <so_luong>` | Mua nhiều item |
| `/buy list` | Xem bảng giá và kho hiện tại |

---

## Hệ thống giá động

Giá thay đổi theo kho server:

**Giá bán** (server trả cho người chơi):
```
f = STOCK_NORMAL / (stock + STOCK_NORMAL × 0.5)
f = clamp(f, 0.3, 2.0)
gia_ban = base × f  (tối thiểu 1 xu)
```

**Giá mua** (người chơi trả cho server):
```
f = STOCK_NORMAL / (stock + STOCK_NORMAL × 0.3) × 1.4
f = clamp(f, 0.5×1.4, 2.5×1.4)
gia_mua = base × f  (tối thiểu 2 xu)
```

- `STOCK_NORMAL = 256` — mức kho coi là bình thường
- `STOCK_MAX = 2048` — giới hạn kho tối đa
- Kho nhiều → giá thấp | Kho ít → giá cao
- Giá mua luôn cao hơn giá bán (chênh ~40%)

---

## HUD Scoreboard

Hiển thị tự động trên thanh sidebar sau khi đăng nhập, cập nhật mỗi 2 giây:

```
§ AuthMod
──────────────
So du: 128 xu
TP tiep: 20xu (lan 2 hom nay)
 
Thi truong (ban/mua/kho)
Wheat        3   5  §c 12 §c▲
Beef         6   9  §e 45 §e~
Carrot       3   4  §a256 §a▼
──────────────
```

Màu kho: `§c` đỏ (< 32) | `§e` vàng (< 256) | `§a` xanh (≥ 256)  
Xu hướng giá: `§c▲` tăng | `§e~` bình thường | `§a▼` giảm

---

## Bedrock (Geyser/Floodgate)

Người chơi Bedrock sử dụng **lệnh giống hệt Java** — không có giao diện form đặc biệt.  
Tên Bedrock có dấu `.` ở đầu (ví dụ `.Steve`), được lưu riêng không xung đột với tài khoản Java `Steve`.

---

## Chạy test

```bash
./gradlew test
# Kết quả: build/reports/tests/test/index.html
```

37 test tổng cộng, không cần Minecraft server để chạy.

---

## Build

```bash
./gradlew build
# Output: build/libs/authmod-1.0.0.jar
```