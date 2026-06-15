import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DbInit {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://192.168.31.100:5432/vectordb";
        String user = "root";
        String pass = "root";
        Path sqlFile = Path.of("src/main/resources/sql/sqldm_v1.0_full_init.sql");

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(true);
            try (Statement st = conn.createStatement()) {
                System.out.println("Dropping schema public...");
                st.execute("DROP SCHEMA IF EXISTS public CASCADE");
                st.execute("CREATE SCHEMA public");
                st.execute("GRANT ALL ON SCHEMA public TO public");
            }

            String script = Files.readString(sqlFile);
            List<String> statements = splitStatements(script);
            System.out.println("Running init script, statements=" + statements.size());
            try (Statement st = conn.createStatement()) {
                for (String sql : statements) {
                    st.execute(sql);
                }
            }

            try (var rs = conn.createStatement().executeQuery(
                    "SELECT id, username, role FROM sys_user ORDER BY id")) {
                System.out.println("Users after init:");
                while (rs.next()) {
                    System.out.println("  " + rs.getInt(1) + " | " + rs.getString(2) + " | " + rs.getString(3));
                }
            }
            System.out.println("Database init OK.");
        }
    }

    private static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (String line : script.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            sb.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String stmt = sb.toString().trim();
                if (!stmt.isEmpty()) {
                    out.add(stmt.substring(0, stmt.length() - 1).trim());
                }
                sb.setLength(0);
            }
        }
        if (!sb.isEmpty()) {
            String stmt = sb.toString().trim();
            if (!stmt.isEmpty()) {
                out.add(stmt.endsWith(";") ? stmt.substring(0, stmt.length() - 1).trim() : stmt);
            }
        }
        return out;
    }
}
