package sophicBinWriteDB;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;

/**
 * This program is to read the local .MD5 archive and store the info into the database
 */
public class sophicBinWriteDB {
    // the JDBC driver
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    // this argument is to visit the database with the style of nodeORlocalhost:port/DBName
    private static String DB_URL = "jdbc:mysql://";
    // the username and password of the database
    private static String USER;
    private static String PASS;
    // the path of .MD5 file
    private static String pathMD5;

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        getArgs(args[0]);
        writeToDB(pathMD5);
    }

    /**
     * read the configuration file to get the arguments needed, as well as the path of the files or the folders which
     * are not wanted to be written into the archive
     *
     * @param arg
     * @throws IOException
     */
    private static void getArgs(String arg) throws IOException {
        // map for args
        HashMap<String, String> argsMap = new HashMap<>();

        BufferedReader bf = new BufferedReader(new FileReader(new File(arg)));
        String line;
        while ((line = bf.readLine()) != null) {
            // skip all comments
            if (line.startsWith("#")) continue;
            int indexEqual = line.indexOf('=');
            argsMap.put(line.substring(0, indexEqual), line.substring(indexEqual + 1));
        }
        bf.close();

        DB_URL += argsMap.get("DB_URL");
        USER = argsMap.get("DB_USER");
        PASS = argsMap.get("DB_PASS");
        pathMD5 = argsMap.get("MD5_PATH");

    }

    /**
     * Write the elements in the treemap into the local archive
     * Connect to the database, and then write the elements in the treemap into the database
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    private static void writeToDB(String pathMD5) throws ClassNotFoundException, SQLException, IOException {
        File MD5File = new File(pathMD5);
        // get the info of BRIF_VER
        String os_version = MD5File.getName().substring(0, MD5File.getName().length() - 4);
        // Connect to the database
        Connection conn;
        Statement stmt;

        Class.forName(JDBC_DRIVER);
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        stmt = conn.createStatement();

        PreparedStatement delete = conn.prepareStatement("delete from 9000_VERINFO_MD5 where BRIF_VER = ?");
        delete.setString(1, os_version);
        delete.executeUpdate();

        String sql = "insert into 9000_VERINFO_MD5 (BRIF_VER,FILEWITHPATH,MD5_INFO) values(?,?,?)";

        BufferedReader bf = new BufferedReader(new FileReader(MD5File));
        String line;
        while ((line = bf.readLine()) != null && line.contains(",")) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, os_version);
            String fwp = line.substring(0, line.indexOf(","));
            ps.setString(2, fwp);
            String md5info = line.substring(line.indexOf(",") + 1);
            ps.setString(3, md5info);
            ps.executeUpdate();
        }
        bf.close();

        stmt.close();
        conn.close();
    }
}
