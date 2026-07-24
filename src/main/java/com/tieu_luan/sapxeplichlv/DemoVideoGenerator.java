package com.tieu_luan.sapxeplichlv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * Creates a reproducible MP4 walkthrough using safe in-memory sample data.
 * Controllers are not initialized, so no database credentials are required.
 */
public class DemoVideoGenerator extends Application {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 8;
    private static final int FRAMES_PER_SCENE = 20;
    private static final Path OUTPUT = Path.of("docs", "demo", "application-demo.mp4");

    private static final List<DemoScene> DEMO_SCENES = List.of(
            new DemoScene("intro_page",
                    "Khởi động ứng dụng và chọn “Bắt đầu sử dụng”",
                    640, 440, 640, 490, DemoVideoGenerator::prepareIntro),
            new DemoScene("login",
                    "Đăng nhập bằng tài khoản quản lý mẫu",
                    570, 300, 640, 455, DemoVideoGenerator::prepareManagerLogin),
            new DemoScene("manager_home",
                    "Quản lý xem và duyệt yêu cầu đổi lịch/nghỉ phép",
                    760, 350, 330, 680, DemoVideoGenerator::prepareManagerHome),
            new DemoScene("manager_employee",
                    "Quản lý danh sách và cập nhật thông tin nhân viên",
                    680, 250, 80, 680, DemoVideoGenerator::prepareEmployees),
            new DemoScene("manager_schedule",
                    "Xếp ca, lọc lịch và xuất báo cáo Excel/PDF",
                    440, 180, 1110, 180, DemoVideoGenerator::prepareSchedule),
            new DemoScene("login",
                    "Chuyển sang tài khoản nhân viên mẫu",
                    570, 300, 640, 455, DemoVideoGenerator::prepareEmployeeLogin),
            new DemoScene("employee_home",
                    "Nhân viên theo dõi lịch làm việc theo tháng",
                    600, 240, 760, 500, DemoVideoGenerator::prepareEmployeeHome),
            new DemoScene("employee_request",
                    "Nhân viên chọn ca và gửi yêu cầu đổi lịch",
                    620, 300, 650, 650, DemoVideoGenerator::prepareRequest),
            new DemoScene("employee_profile",
                    "Cập nhật hồ sơ, đổi mật khẩu và xem lịch sử yêu cầu",
                    520, 330, 700, 610, DemoVideoGenerator::prepareProfile));

    @Override
    public void start(Stage stage) throws Exception {
        Files.createDirectories(OUTPUT.getParent());
        Files.deleteIfExists(OUTPUT);

        SequenceEncoder encoder = SequenceEncoder.createSequenceEncoder(
                OUTPUT.toFile(), FPS);
        try {
            for (int sceneIndex = 0; sceneIndex < DEMO_SCENES.size(); sceneIndex++) {
                DemoScene demo = DEMO_SCENES.get(sceneIndex);
                Preview preview = loadPreview(demo.fxmlName());
                demo.prepare().accept(preview.namespace());

                StackPane frame = createVideoFrame(preview.root(), demo.caption());
                Circle cursor = (Circle) frame.lookup("#demoCursor");
                Scene scene = new Scene(frame, WIDTH, HEIGHT);
                stage.setScene(scene);
                stage.setWidth(WIDTH);
                stage.setHeight(HEIGHT);
                stage.show();

                frame.resize(WIDTH, HEIGHT);
                frame.applyCss();
                frame.layout();

                for (int index = 0; index < FRAMES_PER_SCENE; index++) {
                    double progress = index / (double) (FRAMES_PER_SCENE - 1);
                    double eased = progress * progress * (3 - 2 * progress);
                    cursor.setLayoutX(interpolate(demo.fromX(), demo.toX(), eased));
                    cursor.setLayoutY(interpolate(demo.fromY(), demo.toY(), eased));
                    cursor.setRadius(index >= 15 && index <= 17 ? 17 : 10);

                    WritableImage snapshot = new WritableImage(WIDTH, HEIGHT);
                    frame.snapshot(null, snapshot);
                    encoder.encodeNativeFrame(toPicture(
                            SwingFXUtils.fromFXImage(snapshot, null)));
                }

                System.out.printf("Recorded scene %d/%d: %s%n",
                        sceneIndex + 1, DEMO_SCENES.size(), demo.fxmlName());
            }
        } finally {
            encoder.finish();
            stage.close();
        }

        System.out.println("Created " + OUTPUT);
    }

    private StackPane createVideoFrame(Parent content, String captionText) {
        content.resize(WIDTH, HEIGHT);

        Pane overlay = new Pane();
        overlay.setMouseTransparent(true);

        Label caption = new Label(captionText);
        caption.setLayoutX(24);
        caption.setLayoutY(HEIGHT - 68);
        caption.setPrefWidth(WIDTH - 48);
        caption.setPadding(new Insets(14, 20, 14, 20));
        caption.setStyle("-fx-background-color: rgba(20, 30, 55, 0.90);"
                + "-fx-background-radius: 10; -fx-text-fill: white;"
                + "-fx-font-size: 19px; -fx-font-weight: bold;");

        Circle cursor = new Circle(10, Color.WHITE);
        cursor.setId("demoCursor");
        cursor.setStroke(Color.web("#2457d6"));
        cursor.setStrokeWidth(4);

        Label badge = new Label("DEMO • DỮ LIỆU MẪU");
        badge.setLayoutX(WIDTH - 235);
        badge.setLayoutY(18);
        badge.setPadding(new Insets(7, 12, 7, 12));
        badge.setStyle("-fx-background-color: #f8a427; -fx-background-radius: 12;"
                + "-fx-text-fill: white; -fx-font-weight: bold;");

        overlay.getChildren().addAll(caption, badge, cursor);
        return new StackPane(content, overlay);
    }

    private Preview loadPreview(String fxmlName) throws IOException {
        URL resource = App.class.getResource("views/" + fxmlName + ".fxml");
        if (resource == null) {
            throw new IOException("FXML not found: " + fxmlName);
        }

        String fxml;
        try (var input = resource.openStream()) {
            fxml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        String previewFxml = fxml
                .replaceAll("\\s+fx:controller=\"[^\"]+\"", "")
                .replaceAll("\\s+on[A-Z][A-Za-z]+=\"#[^\"]+\"", "");

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(resource);
        Parent root = loader.load(new ByteArrayInputStream(
                previewFxml.getBytes(StandardCharsets.UTF_8)));
        return new Preview(root, loader.getNamespace());
    }

    private static void prepareIntro(Map<String, Object> namespace) {
        highlight(namespace, "getStartedBtn");
    }

    private static void prepareManagerLogin(Map<String, Object> namespace) {
        setText(namespace, "usernameField", "quanly.demo");
        setText(namespace, "passwordField", "Demo@2026");
        highlight(namespace, "loginBtn");
    }

    private static void prepareEmployeeLogin(Map<String, Object> namespace) {
        setText(namespace, "usernameField", "nhanvien.demo");
        setText(namespace, "passwordField", "Demo@2026");
        highlight(namespace, "loginBtn");
    }

    private static void prepareManagerHome(Map<String, Object> namespace) {
        setText(namespace, "lblWelcome", "Xin chào, Nguyễn Văn An");
        fillTable(namespace, "tblRequests", List.of(
                row("YC001", "Nguyễn Thuỳ Dung", "Đổi ca", "24/07/2026",
                        "24/07/2026", "Chờ duyệt"),
                row("YC002", "Phạm Văn Tú", "Nghỉ phép", "27/07/2026",
                        "28/07/2026", "Chờ duyệt"),
                row("YC003", "Vũ Thị Hoa", "Đổi ca", "30/07/2026",
                        "30/07/2026", "Đã duyệt")));
        enable(namespace, "btnApprove");
        enable(namespace, "btnReject");
        highlight(namespace, "btnApprove");
    }

    private static void prepareEmployees(Map<String, Object> namespace) {
        fillTable(namespace, "tblEmployees", List.of(
                row("NV001", "Nguyễn Văn An", "123456789012", "0987654321",
                        "nva@example.com", "Nam", "Quản lý", "quanly.demo", "Đang làm"),
                row("NV002", "Nguyễn Thuỳ Dung", "212456989012", "0627652321",
                        "ntd@example.com", "Nữ", "Thu ngân", "dungnt", "Đang làm"),
                row("NV003", "Phạm Văn Tú", "523456789012", "0947654321",
                        "pvt@example.com", "Nam", "Phục vụ", "nhanvien.demo", "Đang làm")));
        setText(namespace, "txtHoTen", "Lê Minh Anh");
        setText(namespace, "txtEmail", "minhanh@example.com");
        setCombo(namespace, "cmbGioiTinh", "Nữ");
        setCombo(namespace, "cmbViTri", "Phục vụ");
        highlight(namespace, "btnAdd");
    }

    private static void prepareSchedule(Map<String, Object> namespace) {
        setDate(namespace, "datePicker", LocalDate.of(2026, 7, 25));
        setCombo(namespace, "cmbViewType", "Theo ngày");
        setCombo(namespace, "cmbEmployee", "Nguyễn Thuỳ Dung");
        setCombo(namespace, "cmbShift", "Ca chiều (13:00 - 17:00)");
        fillTable(namespace, "tblSchedule", List.of(
                row("25/07/2026", "Ca sáng", "Nguyễn Văn An", "Quản lý"),
                row("25/07/2026", "Ca sáng", "Phạm Văn Tú", "Phục vụ"),
                row("25/07/2026", "Ca chiều", "Nguyễn Thuỳ Dung", "Thu ngân"),
                row("25/07/2026", "Ca tối", "Vũ Thị Hoa", "Phục vụ")));
        highlight(namespace, "btnExportPDF");
    }

    private static void prepareEmployeeHome(Map<String, Object> namespace) {
        setText(namespace, "lblWelcome", "Xin chào, Phạm Văn Tú");
        setText(namespace, "lblMonthYear", "Tháng 7/2026");

        Object value = namespace.get("calendarGrid");
        if (value instanceof GridPane calendar) {
            calendar.getChildren().clear();
            for (int day = 1; day <= 31; day++) {
                VBox card = new VBox(4);
                card.setPadding(new Insets(6));
                card.setStyle("-fx-background-color: white; -fx-border-color: #d7ddeb;"
                        + "-fx-background-radius: 5; -fx-border-radius: 5;");
                Label dayLabel = new Label(String.valueOf(day));
                dayLabel.setStyle("-fx-font-weight: bold;");
                card.getChildren().add(dayLabel);
                if (day % 3 == 1) {
                    Label shift = new Label(day % 2 == 0 ? "Ca chiều" : "Ca sáng");
                    shift.setStyle("-fx-background-color: #4f73df; -fx-text-fill: white;"
                            + "-fx-padding: 3 7; -fx-background-radius: 4;");
                    card.getChildren().add(shift);
                }
                calendar.add(card, (day - 1) % 7, (day - 1) / 7);
            }
        }
    }

    private static void prepareRequest(Map<String, Object> namespace) {
        setCombo(namespace, "cmbShift", "25/07/2026 - Ca sáng");
        setText(namespace, "lblCurrentShift", "Ca sáng (06:00 - 12:00)");
        setCombo(namespace, "cmbEmployee", "Vũ Thị Hoa - Ca chiều");
        highlight(namespace, "btnSubmit");
    }

    private static void prepareProfile(Map<String, Object> namespace) {
        setText(namespace, "txtMaNV", "NV003");
        setText(namespace, "txtHoTen", "Phạm Văn Tú");
        setText(namespace, "txtCCCD", "523456789012");
        setText(namespace, "txtSDT", "0947654321");
        setText(namespace, "txtEmail", "pvt@example.com");
        setText(namespace, "txtViTri", "Phục vụ");
        setText(namespace, "txtTenDN", "nhanvien.demo");
        fillTable(namespace, "tblRequests", List.of(
                row("YC002", "Nghỉ phép", "27/07/2026", "28/07/2026", "Chờ duyệt"),
                row("YC003", "Đổi ca", "20/07/2026", "20/07/2026", "Đã duyệt")));
        highlight(namespace, "btnUpdate");
    }

    @SuppressWarnings("unchecked")
    private static void fillTable(
            Map<String, Object> namespace,
            String id,
            List<List<String>> rows) {
        Object value = namespace.get(id);
        if (!(value instanceof TableView<?> rawTable)) {
            return;
        }

        TableView<List<String>> table = (TableView<List<String>>) rawTable;
        List<TableColumn<List<String>, ?>> columns = table.getColumns();
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            final int index = columnIndex;
            TableColumn<List<String>, String> column =
                    (TableColumn<List<String>, String>) columns.get(columnIndex);
            column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                    index < cell.getValue().size() ? cell.getValue().get(index) : ""));
        }
        table.setItems(FXCollections.observableArrayList(rows));
        if (!rows.isEmpty()) {
            table.getSelectionModel().selectFirst();
        }
    }

    private static List<String> row(String... values) {
        return Arrays.asList(values);
    }

    private static void setText(Map<String, Object> namespace, String id, String text) {
        Object value = namespace.get(id);
        if (value instanceof TextInputControl input) {
            input.setText(text);
        } else if (value instanceof Label label) {
            label.setText(text);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setCombo(Map<String, Object> namespace, String id, String text) {
        Object value = namespace.get(id);
        if (value instanceof ComboBox<?> rawCombo) {
            ComboBox<String> combo = (ComboBox<String>) rawCombo;
            combo.setItems(FXCollections.observableArrayList(text));
            combo.setValue(text);
        }
    }

    private static void setDate(
            Map<String, Object> namespace,
            String id,
            LocalDate date) {
        Object value = namespace.get(id);
        if (value instanceof DatePicker picker) {
            picker.setValue(date);
        }
    }

    private static void enable(Map<String, Object> namespace, String id) {
        Object value = namespace.get(id);
        if (value instanceof Node node) {
            node.setDisable(false);
        }
    }

    private static void highlight(Map<String, Object> namespace, String id) {
        Object value = namespace.get(id);
        if (value instanceof Button button) {
            button.setStyle(button.getStyle()
                    + "; -fx-border-color: #f8a427; -fx-border-width: 3;"
                    + "-fx-border-radius: 5;");
        }
    }

    private static double interpolate(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static Picture toPicture(BufferedImage image) {
        Picture picture = Picture.create(image.getWidth(), image.getHeight(), ColorSpace.RGB);
        byte[] data = picture.getPlaneData(0);
        int offset = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                data[offset++] = (byte) (((rgb >> 16) & 0xff) - 128);
                data[offset++] = (byte) (((rgb >> 8) & 0xff) - 128);
                data[offset++] = (byte) ((rgb & 0xff) - 128);
            }
        }
        return picture;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record Preview(Parent root, Map<String, Object> namespace) {
    }

    private record DemoScene(
            String fxmlName,
            String caption,
            double fromX,
            double fromY,
            double toX,
            double toY,
            Consumer<Map<String, Object>> prepare) {
    }
}
