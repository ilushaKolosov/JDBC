import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class JDBC {
    private static final Config config = new Config();
    private static final DataSource dataSource = config.getDataSource();

    public static void main(String[] args) {
        insertDataIntoVacationBatch();
    }

    public static List<Vacation> retrieveVacations() {
        List<Vacation> vacations = new ArrayList<>();
        String sql = "SELECT vacation_id, employee_name, start_date, end_date, vacation_type, approved " +
                "FROM Vacation " +
                "WHERE AGE(end_date, start_date) < INTERVAL '10 days' " +
                "ORDER BY start_date DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Vacation vacation = new Vacation();
                vacation.setVacationId(rs.getInt("vacation_id"));
                vacation.setEmployeeName(rs.getString("employee_name"));
                vacation.setStartDate(rs.getDate("start_date"));
                vacation.setEndDate(rs.getDate("end_date"));
                vacation.setVacationType(rs.getString("vacation_type"));
                vacation.setApproved(rs.getBoolean("approved"));
                vacations.add(vacation);
            }

        } catch (SQLException e) {
            log.error("Something was going wrong!");
            e.printStackTrace();
        }
        return vacations;
    }


    public static void createVacationDetailsView(Connection conn) throws SQLException {
        String sql = "CREATE OR REPLACE VIEW VacationDetails AS " +
                "SELECT vacation_id, employee_name, start_date, end_date, vacation_type, approved " +
                "FROM Vacation";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Something was going wrong!");
            e.printStackTrace();
        }
    }


    public static void createVacationLogTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE VacationLog (" +
                "    log_id serial PRIMARY KEY, " +
                "    operation_name varchar(50) NOT NULL, " +
                "    vacation_id integer, " +
                "    employee_name varchar(100), " +
                "    start_date date, " +
                "    end_date date, " +
                "    vacation_type varchar(50), " +
                "    approved boolean, " +
                "    operation_date timestamp " +
                ");";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Something was going wrong!");
            e.printStackTrace();
        }
    }



    public static void createInsertTrigger(Connection conn) throws SQLException {
        String triggerSql = "CREATE OR REPLACE FUNCTION log_vacation_insert() RETURNS TRIGGER AS $$\n" +
                "BEGIN\n" +
                "    INSERT INTO VacationLog (operation_name, vacation_id, employee_name, start_date, end_date, vacation_type, approved, operation_date)\n" +
                "    VALUES ('INSERT', NEW.vacation_id, NEW.employee_name, NEW.start_date, NEW.end_date, NEW.vacation_type, NEW.approved, CURRENT_TIMESTAMP);\n" +
                "    RETURN NEW;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql;\n" +
                "\n" +
                "CREATE TRIGGER tr_vacation_insert\n" +
                "AFTER INSERT ON Vacation\n" +
                "FOR EACH ROW\n" +
                "EXECUTE FUNCTION log_vacation_insert();";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(triggerSql);
        }  catch (SQLException e) {
            log.error("Something was going wrong!");
            e.printStackTrace();
        }
    }

    public static void createUpdateTrigger(Connection conn) throws SQLException {
        String triggerSql = "CREATE OR REPLACE FUNCTION log_vacation_update() RETURNS TRIGGER AS $$\n" +
                "BEGIN\n" +
                "    INSERT INTO VacationLog (operation_name, vacation_id, employee_name, start_date, end_date, vacation_type, approved, operation_date)\n" +
                "    VALUES ('UPDATE', NEW.vacation_id, NEW.employee_name, NEW.start_date, NEW.end_date, NEW.vacation_type, NEW.approved, CURRENT_TIMESTAMP);\n" +
                "    RETURN NEW;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql;\n" +
                "\n" +
                "CREATE TRIGGER tr_vacation_update\n" +
                "AFTER UPDATE ON Vacation\n" +
                "FOR EACH ROW\n" +
                "WHEN (OLD.* IS DISTINCT FROM NEW.*)\n" +
                "EXECUTE FUNCTION log_vacation_update();";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(triggerSql);
        }  catch (SQLException e) {
            log.error("Something was going wrong!");
            e.printStackTrace();
        }
    }

    public static void createDeleteTrigger(Connection conn) throws SQLException {
        String triggerSql = "CREATE OR REPLACE FUNCTION log_vacation_delete() RETURNS TRIGGER AS $$\n" +
                "BEGIN\n" +
                "    INSERT INTO VacationLog (operation_name, vacation_id, employee_name, start_date, end_date, vacation_type, approved, operation_date)\n" +
                "    VALUES ('DELETE', OLD.vacation_id, OLD.employee_name, OLD.start_date, OLD.end_date, OLD.vacation_type, OLD.approved, CURRENT_TIMESTAMP);\n" +
                "    RETURN OLD;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql;\n" +
                "\n" +
                "CREATE TRIGGER tr_vacation_delete\n" +
                "AFTER DELETE ON Vacation\n" +
                "FOR EACH ROW\n" +
                "EXECUTE FUNCTION log_vacation_delete();";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(triggerSql);
        }  catch (SQLException e) {
            log.error("Something was going wrong!");
            e.printStackTrace();
        }
    }

    public static void createVacationTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE Vacation ( " +
                "    vacation_id serial PRIMARY KEY, " +
                "    employee_name varchar(100) NOT NULL, " +
                "    start_date date NOT NULL, " +
                "    end_date date NOT NULL, " +
                "    vacation_type varchar(50), " +
                "    approved boolean DEFAULT false " +
                ");";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Something was going wrong!");
            e.printStackTrace();
        }
    }

    public static void insertDataIntoVacationBatch() {
        String sql = "INSERT INTO Vacation (employee_name, start_date, end_date, vacation_type, approved) " +
                "VALUES (?, ?, ?, ?, ?)";

        String[] employeeNames = {"John Doe", "Jane Smith", "Michael Johnson", "Emily Brown"};
        String[] vacationTypes = {"Paid", "Unpaid", "Sick"};

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Random random = new Random();

            conn.setAutoCommit(false);

            for (int i = 0; i < 10; i++) {
                String employeeName = employeeNames[random.nextInt(employeeNames.length)];
                Date startDate = getRandomDate();
                Date endDate = getRandomDate();
                String vacationType = vacationTypes[random.nextInt(vacationTypes.length)];
                boolean approved = random.nextBoolean();

                pstmt.setString(1, employeeName);
                pstmt.setDate(2, startDate);
                pstmt.setDate(3, endDate);
                pstmt.setString(4, vacationType);
                pstmt.setBoolean(5, approved);

                pstmt.addBatch();
            }

            int[] result = pstmt.executeBatch();

            conn.commit();
            conn.setAutoCommit(true);

            log.info("Successfully inserted {} rows.", result.length);

        } catch (SQLException e) {
            log.error("Batch insert failed!");
            e.printStackTrace();
        }
    }


    public static void executeInsertAndGetResult() {
        String sql = "INSERT INTO Vacation (employee_name, start_date, end_date, vacation_type, approved) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "John Doe");
            pstmt.setDate(2, Date.valueOf("2023-06-15"));
            pstmt.setDate(3, Date.valueOf("2023-06-20"));
            pstmt.setString(4, "Paid");
            pstmt.setBoolean(5, true);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                log.info("INSERT successful. {} row(s) affected.", rowsAffected);
            } else {
                log.warn("INSERT did not affect any rows.");
            }

        } catch (SQLException e) {
            log.error("Failed to execute INSERT statement.");
            e.printStackTrace();
        }
    }


    public static void executeUpdateAndGetResult() {
        String sql = "UPDATE Vacation SET vacation_type = ? WHERE employee_name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "Unpaid");
            pstmt.setString(2, "John Doe");

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                log.info("UPDATE successful. {} row(s) updated.", rowsAffected);
            } else {
                log.warn("UPDATE did not affect any rows.");
            }

        } catch (SQLException e) {
            log.error("Failed to execute UPDATE statement.");
            e.printStackTrace();
        }
    }


    private static Date getRandomDate() {
        Random random = new Random();
        int year = 2023 + random.nextInt(2024 - 2023 + 1);
        int month = random.nextInt(12) + 1;
        int day = random.nextInt(28) + 1;

        return Date.valueOf(year + "-" + month + "-" + day);
    }
}
