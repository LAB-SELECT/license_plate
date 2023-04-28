package com.db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConnectDB {
    private static ConnectDB instance = new ConnectDB();

    public static ConnectDB getInstance() {
        return instance;
    }
    public ConnectDB() {  }

    // oracle 계정
    String jdbcUrl = "jdbc:oracle:thin:@IPv4주소:1521:testdb";
    String userId = "test";
    String userPw = "test";

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    String sql = "";
    String returns = "a";

    public String connectionDB(String detectNum) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection(jdbcUrl, userId, userPw);

            sql = "SELECT id FROM plateNumTable WHERE plateNum = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, detectNum);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                returns = "DB에 존재하는 차량";
            } else {
                returns = "DB에 존재하지 않는 차량";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pstmt != null)try {pstmt.close();} catch (SQLException ex) {}
            if (conn != null)try {conn.close();    } catch (SQLException ex) {    }
        }
        return returns;
    }
}