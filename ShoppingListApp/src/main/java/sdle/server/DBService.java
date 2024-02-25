package sdle.server;

import java.sql.*;
import java.util.ArrayList;

public class DBService {

    public Connection conn = null;
    public Connection connect(String nodeId) {
        this.conn = null;
        String dbPath = "src/main/java/sdle/server/DB/" + nodeId + ".db";
        try {
            String url = "jdbc:sqlite:" + dbPath;
            this.conn = DriverManager.getConnection(url);

            if (!tableExists("shopping_list")) {
                createTable();
            }

        } catch (SQLException e) {
            System.out.println("AQUI");
            System.out.println(e.getMessage());
            this.closeConnection();
        }
        return conn;
    }
    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet resultSet = metaData.getTables(null, null, tableName, null);
        return resultSet.next();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS shopping_list (\n"
                + "    id VARCHAR(256) PRIMARY KEY,\n"
                + "    list BLOBs\n"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            this.closeConnection();
            System.out.println(e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public byte[] getShoppingList(String listId) {

        ResultSet resultSet = null;

        byte[] shoppingListBytes = null ;

        String sql = "SELECT * FROM shopping_list WHERE id = ?";

        try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            pstmt.setString(1, listId);
            resultSet = pstmt.executeQuery();

            // Assuming 'item' is the column name for shopping list items in your ResultSet
            while (resultSet.next()) {
                shoppingListBytes = resultSet.getBytes("list");
            }

        } catch (SQLException e) {
            this.closeConnection();
            e.printStackTrace();

        }

        return shoppingListBytes;
    }
    public ArrayList<String> getShoppingListIds() {
        ArrayList<String> shoppingListIds = new ArrayList<>();

        String sql = "SELECT id FROM shopping_list";

        try (Statement stmt = conn.createStatement();
             ResultSet resultSet = stmt.executeQuery(sql)) {

            while (resultSet.next()) {
                String id = resultSet.getString("id");
                shoppingListIds.add(id);
            }

        } catch (SQLException e) {
            this.closeConnection();
            System.out.println(e.getMessage());
        }

        return shoppingListIds;
    }
    public Boolean upsertShoppingList(String listId, byte[] newShoppingListBytes) {
        boolean upserted = false;

        if (newShoppingListBytes == null) {
            // Handle the case where newShoppingListBytes is null
            return upserted;
        }

        String sql = "INSERT OR REPLACE INTO shopping_list (id, list) VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, listId);
            pstmt.setBytes(2, newShoppingListBytes);


            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                upserted = true;
            }
        } catch (SQLException e) {
            this.closeConnection();
            e.printStackTrace();
            return false;
        }

        return upserted;
    }

    public Boolean deleteShoppingList(String listId){
        boolean deleted = false;
        String sql = "DELETE FROM shopping_list WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, listId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                deleted = true;
            }
        } catch (SQLException e) {
            this.closeConnection();
            e.printStackTrace();
        }
        return deleted;

    }

    public static void main(String[] args) {
        DBService service = new DBService();
        service.connect("node1");
        service.deleteShoppingList("comprasFDS");
        service.closeConnection();
    }
}
