CREATE DATABASE QuanLyLichLamViec
GO

USE QuanLyLichLamViec
GO

--------------- Tạo bảng ---------------
 
-- Bảng Vị trí công việc
CREATE TABLE ViTriCV (
    MaVT INT IDENTITY(1,1) PRIMARY KEY,
    TenVT NVARCHAR(20) NOT NULL,
    MoTa NVARCHAR(1000) NULL
)
GO

-- Bảng Nhân viên
CREATE TABLE NhanVien (
    MaNV INT IDENTITY(1,1) PRIMARY KEY,
    HoTen NVARCHAR(30) NOT NULL,
    CCCD VARCHAR(12) NOT NULL,
    SDT VARCHAR(10) NOT NULL,
    Email VARCHAR(30) NULL,
    GioiTinh BIT NULL,  -- 0: Nữ, 1: Nam
    MaVT INT NOT NULL,
    TenDN VARCHAR(30) NOT NULL UNIQUE,
    MatKhau VARCHAR(255) NOT NULL DEFAULT '123',
    TrangThai TINYINT NOT NULL CHECK (TrangThai IN (0, 1, 2)) DEFAULT 1,  -- 0: Đã nghỉ, 1: Đang làm, 2: Nghỉ phép
    SoNgayNghiThang TINYINT NOT NULL DEFAULT 0,  -- Số ngày nghỉ trong tháng (tối đa 4)
    FOREIGN KEY (MaVT) REFERENCES ViTriCV(MaVT)
)
GO

-- Bảng Ca làm việc
CREATE TABLE Ca (
    MaCa INT IDENTITY(1,1) PRIMARY KEY,
    TenCa NVARCHAR(20) NOT NULL,
    GioBD TIME NOT NULL,
    GioKT TIME NOT NULL
)
GO

-- Bảng Lịch làm việc
CREATE TABLE LichLV (
    MaLich INT IDENTITY(1,1) PRIMARY KEY,
    MaNV INT NOT NULL,
    MaCa INT NULL,
    NgayLam DATE NOT NULL,
    FOREIGN KEY (MaNV) REFERENCES NhanVien(MaNV),
    FOREIGN KEY (MaCa) REFERENCES Ca(MaCa)
)
GO

-- Bảng Yêu cầu đổi lịch
CREATE TABLE YeuCauDL (
    MaYC INT IDENTITY(1,1) PRIMARY KEY,
    MaNV INT NOT NULL,
    LoaiYC TINYINT NOT NULL CHECK (LoaiYC IN (0, 1)), -- 0: Nghỉ phép, 1: Đổi ca
    MaLich INT NULL,
    NhanVienDoi INT NULL, -- Nhân viên muốn đổi ca
	NgayBatDau DATE,   -- Ngày bắt đầu nghỉ phép (nếu là yêu cầu nghỉ phép)
    NgayKetThuc DATE,  -- Ngày kết thúc nghỉ phép (nếu là yêu cầu nghỉ phép)
    TrangThai TINYINT NOT NULL CHECK (TrangThai IN (0, 1, 2)), -- 0: Chờ duyệt, 1: Chấp nhận, 2: Từ chối
    FOREIGN KEY (MaNV) REFERENCES NhanVien(MaNV),
    FOREIGN KEY (MaLich) REFERENCES LichLV(MaLich),
    FOREIGN KEY (NhanVienDoi) REFERENCES NhanVien(MaNV)
)
GO

--------------- Thêm dữ liệu mẫu ---------------
-- Thêm dữ liệu vào bảng Vị trí công việc
INSERT INTO ViTriCV (TenVT, MoTa) VALUES
(N'Quản lý', N'Quản lý chung nhà hàng'),
(N'Bếp trưởng', N'Phụ trách nấu ăn và điều phối bếp'),
(N'Phụ bếp', N'Hỗ trợ bếp trưởng chuẩn bị món ăn'),
(N'Thu ngân', N'Quản lý thanh toán và hóa đơn'),
(N'Phục vụ', N'Tiếp nhận và phục vụ khách hàng'),
(N'Bảo vệ', N'Đảm bảo an ninh cho nhà hàng')
GO

-- Thêm dữ liệu vào bảng Nhân viên
INSERT INTO NhanVien (HoTen, CCCD, SDT, Email, GioiTinh, MaVT, TenDN, MatKhau, TrangThai, SoNgayNghiThang) VALUES
(N'Nguyễn Thị Hoà', '123456789022', '0987654342', 'nth@example.com', 0, 2, 'hant', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Đoàn Văn Hứa', '223456789020', '0727654351', 'dvh@example.com', 1, 3, 'huadv', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Nguyễn Thuỳ Dung', '212456989012', '0627652321', 'ntd@example.com', 0, 4, 'dungnt', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Nguyễn Văn An', '123456789012', '0987654321', 'nva@example.com', 1, 1, 'nguyenvana', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Trần Thảo Quỳnh', '223456789012', '0977654321', 'ttb@example.com', 0, 2, 'tranthib', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Lê Văn Cung', '323456789012', '0967654321', 'lvc@example.com', 1, 3, 'levanc', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Hoàng Thị Dung', '423456789012', '0957654321', 'htd@example.com', 0, 4, 'hoangthid', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Phạm Văn Tú', '523456789012', '0947654321', 'pve@example.com', 1, 5, 'phamvane', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Vũ Thị Hoa', '623456789012', '0937654321', 'vtf@example.com', 0, 5, 'vuthif', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Đặng Văn Cương', '723456789012', '0927654321', 'dvg@example.com', 1, 5, 'dangvang', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Lý Thị Hương', '823456789012', '0917654321', 'lth@example.com', 0, 6, 'lythih', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Bùi Văn Khoa', '923456789012', '0907654321', 'bvk@example.com', 1, 6, 'buivank', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0),
(N'Ngô Thị Mai', '102345678901', '0897654321', 'ntm@example.com', 0, 5, 'ngothim', '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa', 1, 0)
GO
update NhanVien set MatKhau = '$2a$12$U5SwwKIw.AGJigSy5S/q8eSt0BAHDP9BjgdCzQXL/RzHmxD1NDJQa'

-- Thêm dữ liệu vào bảng Ca làm việc
INSERT INTO Ca (TenCa, GioBD, GioKT) VALUES
(N'Ca sáng', '06:00', '12:00'),
(N'Ca chiều', '13:00', '17:00'),
(N'Ca tối', '18:00', '22:00')
GO

-- Thêm dữ liệu vào bảng Lịch làm việc
--INSERT INTO LichLV (MaNV, MaCa, NgayLam, TrangThai) VALUES
--(1, 1, '2025-03-01', 0),
--(2, 2, '2025-03-01', 0),
--(3, 3, '2025-03-01', 0),
--(4, 1, '2025-03-02', 0),
--(5, 2, '2025-03-02', 0),
--(6, 3, '2025-03-02', 0),
--(7, 1, '2025-03-03', 0),
--(8, 2, '2025-03-03', 0),
--(9, 3, '2025-03-03', 0),
--(10, 1, '2025-03-04', 0);
--GO

---- Thêm dữ liệu vào bảng Yêu cầu đổi lịch
--INSERT INTO YeuCauDL (MaNV, LoaiYC, MaLich, TrangThai) VALUES
--(3, 0, NULL, 0), -- Nhân viên 3 xin nghỉ phép
--(5, 1, 2, 0), -- Nhân viên 5 muốn đổi ca ngày 2025-03-01
--(7, 1, 7, 0); -- Nhân viên 7 muốn đổi ca ngày 2025-03-03
--GO


--CREATE PROCEDURE SapXepLich_Greedy
--AS
--BEGIN
--    DECLARE @NgayBatDau DATE, @NgayKetThuc DATE
--    SET @NgayBatDau = DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1)
--    SET @NgayKetThuc = EOMONTH(@NgayBatDau)

--    DECLARE @Ngay DATE = @NgayBatDau
--    WHILE @Ngay <= @NgayKetThuc
--    BEGIN
--        -- Danh sách nhân viên có thể làm việc trong ngày
--        DECLARE @NhanVien TABLE (MaNV INT, MaVT INT, SoCaDaLam INT DEFAULT 0)

--        INSERT INTO @NhanVien (MaNV, MaVT, SoCaDaLam)
--        SELECT nv.MaNV, nv.MaVT, 
--               (SELECT COUNT(*) FROM LichLV WHERE MaNV = nv.MaNV AND NgayLam = @Ngay) 
--        FROM NhanVien nv WHERE TrangThai = 1

--        -- Chia ca làm việc
--        DECLARE @Ca TABLE (MaCa INT, TenCa NVARCHAR(20))
--        INSERT INTO @Ca SELECT MaCa, TenCa FROM Ca

--        DECLARE @MaCa INT, @TenCa NVARCHAR(20)
--        DECLARE CaCursor CURSOR FOR SELECT MaCa, TenCa FROM @Ca
--        OPEN CaCursor

--        FETCH NEXT FROM CaCursor INTO @MaCa, @TenCa
--        WHILE @@FETCH_STATUS = 0
--        BEGIN
--            -- Xác định số lượng nhân viên cần cho ca này
--            DECLARE @SoLuongPhucVu INT = 1, @CanBepTruong INT = 0, @CanPhuBep INT = 0, @CanThuNgan INT = 1, @CanBaoVe INT = 1
            
--            IF @TenCa = N'Ca tối' 
--            BEGIN
--                SET @SoLuongPhucVu = 2
--                SET @CanBepTruong = 1
--                SET @CanPhuBep = 1
--            END
--            ELSE
--            BEGIN
--                SET @CanBepTruong = 1
--            END

--            -- Danh sách nhân viên làm việc trong ca này
--            DECLARE @Lich TABLE (MaNV INT, MaCa INT)

--            -- Chọn nhân viên phục vụ
--            INSERT INTO @Lich (MaNV, MaCa)
--            SELECT TOP (@SoLuongPhucVu) MaNV, @MaCa 
--            FROM @NhanVien nv
--            WHERE MaVT = (SELECT MaVT FROM ViTriCV WHERE TenVT = N'Phục vụ') 
--                  AND SoCaDaLam < 2
--                  AND NOT EXISTS (SELECT 1 FROM LichLV WHERE MaNV = nv.MaNV AND NgayLam = @Ngay)
--            ORDER BY SoCaDaLam ASC, NEWID()

--            -- Chọn bếp trưởng nếu cần
--            IF @CanBepTruong = 1
--                INSERT INTO @Lich (MaNV, MaCa)
--                SELECT TOP 1 MaNV, @MaCa 
--                FROM @NhanVien nv
--                WHERE MaVT = (SELECT MaVT FROM ViTriCV WHERE TenVT = N'Bếp trưởng') 
--                      AND SoCaDaLam < 2
--                      AND NOT EXISTS (SELECT 1 FROM LichLV WHERE MaNV = nv.MaNV AND NgayLam = @Ngay)
--                ORDER BY SoCaDaLam ASC, NEWID()

--            -- Chọn phụ bếp nếu cần
--            IF @CanPhuBep = 1
--                INSERT INTO @Lich (MaNV, MaCa)
--                SELECT TOP 1 MaNV, @MaCa 
--                FROM @NhanVien nv
--                WHERE MaVT = (SELECT MaVT FROM ViTriCV WHERE TenVT = N'Phụ bếp') 
--                      AND SoCaDaLam < 2
--                      AND NOT EXISTS (SELECT 1 FROM LichLV WHERE MaNV = nv.MaNV AND NgayLam = @Ngay)
--                ORDER BY SoCaDaLam ASC, NEWID()

--            -- Chọn thu ngân nếu cần
--            IF @CanThuNgan = 1
--                INSERT INTO @Lich (MaNV, MaCa)
--                SELECT TOP 1 MaNV, @MaCa 
--                FROM @NhanVien nv
--                WHERE MaVT = (SELECT MaVT FROM ViTriCV WHERE TenVT = N'Thu ngân') 
--                      AND SoCaDaLam < 2
--                      AND NOT EXISTS (SELECT 1 FROM LichLV WHERE MaNV = nv.MaNV AND NgayLam = @Ngay)
--                ORDER BY SoCaDaLam ASC, NEWID()

--            -- Chọn bảo vệ nếu cần
--            IF @CanBaoVe = 1
--                INSERT INTO @Lich (MaNV, MaCa)
--                SELECT TOP 1 MaNV, @MaCa 
--                FROM @NhanVien nv
--                WHERE MaVT = (SELECT MaVT FROM ViTriCV WHERE TenVT = N'Bảo vệ') 
--                      AND SoCaDaLam < 2
--                      AND NOT EXISTS (SELECT 1 FROM LichLV WHERE MaNV = nv.MaNV AND NgayLam = @Ngay)
--                ORDER BY SoCaDaLam ASC, NEWID()

--            -- Kiểm tra xem lịch làm việc có bị trùng không trước khi thêm vào bảng chính
--            INSERT INTO LichLV (MaNV, MaCa, NgayLam)
--            SELECT MaNV, MaCa, @Ngay 
--            FROM @Lich l 
--            WHERE NOT EXISTS (SELECT 1 FROM LichLV WHERE MaNV = l.MaNV AND NgayLam = @Ngay AND MaCa = l.MaCa)

--            -- Cập nhật số ca đã làm của nhân viên
--            UPDATE nv
--            SET nv.SoCaDaLam = nv.SoCaDaLam + 1
--            FROM @NhanVien nv
--            INNER JOIN @Lich l ON nv.MaNV = l.MaNV

--            FETCH NEXT FROM CaCursor INTO @MaCa, @TenCa
--        END

--        CLOSE CaCursor
--        DEALLOCATE CaCursor

--        -- Tiếp tục sang ngày tiếp theo
--        SET @Ngay = DATEADD(DAY, 1, @Ngay)
--    END
--END
--GO


--exec SapXepLich_Greedy
--go

--select * from LichLV

select * from NhanVien
--select * from Ca
--select * from ViTriCV