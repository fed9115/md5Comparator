import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.sql.*;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This program is to generate the archive file of one large software project folder containing thousands of files in
 * two styles: local .MD5 file and remote database table.
 *
 * @author Erdi Fan
 * @date July 4th, 2019
 */

public class archiveGenerator {
    // two tree maps
    private static TreeMap<String, String> map = new TreeMap<>();
    private static TreeSet<File> ignoredFiles = new TreeSet<>();

    // the path of the software
    private static String path;
    // the path of the save folder locally
    private static String savepathInitial;
    // the JDBC driver
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    // this argument is to visit the database with the style of nodeORlocalhost:port/DBName
    private static String DB_URL = "jdbc:mysql://";
    // the username and password of the database
    private static String USER;
    private static String PASS;

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        // get the arguments
        getArgs(args[0]);

        // Prepare the save folder
        File saveFolder = new File(savepathInitial);
        saveFolder.mkdirs();

        // the first argument is the one for the version folder
        File version = new File(path);

        File[] arr = version.listFiles();

        // recursively find the files till the bottom level
        recursiveFinding(arr, 0, map);

        String os_version = getOSVersion(path + "version.txt");

        // write to the local archive as well as the database
        FileWriter fw = new FileWriter(new File(savepathInitial + os_version + ".MD5"));
        writeArchiveAndDB(fw, os_version, map);
        fw.write("一共有 " + map.size() + " 个文件被归档");
        fw.close();

    }

    /**
     * read the configuration file to get the arguments needed, as well as the path of the files or the folders which
     * are not wanted to be written into the archive
     *
     * @param arg
     * @throws IOException
     */
    private static void getArgs(String arg) throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(new File(arg)));
        path = bf.readLine();
        savepathInitial = bf.readLine();
        DB_URL += bf.readLine();
        USER = bf.readLine();
        PASS = bf.readLine();
        String line;
        while ((line = bf.readLine()) != null) {
            File f = new File(path + line);
            ignoredFiles.add(f);
        }
        bf.close();
    }

    /**
     * recursively find the files which needed to be written to the archive, store them into the treemap
     *
     * @param arr
     * @param index
     * @param map
     * @throws IOException
     */
    private static void recursiveFinding(File[] arr, int index, TreeMap<String, String> map) throws IOException {
        if (index == arr.length) return;
        if (arr[index].isFile() && !ignoredFiles.contains(arr[index])) {
            if (!arr[index].getName().equals("version.txt")) {
                FileInputStream fis = new FileInputStream(arr[index]);
                String currMd5 = DigestUtils.md5Hex(fis);
                fis.close();
                addToMap(arr[index].getPath(), currMd5, map);
            }
        } else if (arr[index].isDirectory() && !ignoredFiles.contains(arr[index])) {
            // recursion for sub-directories
            recursiveFinding(arr[index].listFiles(), 0, map);
        }
        // recursion for main directory
        recursiveFinding(arr, ++index, map);
    }

    /**
     * get the opreating system and version info from version.txt
     *
     * @param versionPath
     * @return
     * @throws IOException
     */
    private static String getOSVersion(String versionPath) throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(versionPath));
        bf.readLine();
        String versionLine = bf.readLine().substring(4);
        bf.close();
        return versionLine;
    }

    /**
     * Helper method to avoid writing down soft links in the archive files: add it to the map
     *
     * @param currPath
     */
    private static void addToMap(String currPath, String md5, TreeMap<String, String> map) {

        if (currPath.contains(".so")) {
            currPath = currPath.substring(0, currPath.indexOf(".so") + 3);
        }

        int idxOfSlash = currPath.indexOf('/');

        String neededPath = currPath.substring(idxOfSlash + 1);

        map.put(neededPath, md5);
    }

    /**
     *  Write the elements in the treemap into the local archive
     *  Connect to the database, and then write the elements in the treemap into the database
     *
     * @param fw
     * @param os_version
     * @param map
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    private static void writeArchiveAndDB(FileWriter fw, String os_version, TreeMap<String, String> map) throws ClassNotFoundException,
            SQLException, IOException {
        // Connect to the database
        Connection conn;
        Statement stmt;

        Class.forName(JDBC_DRIVER);
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        stmt = conn.createStatement();

        PreparedStatement pst = conn.prepareStatement("insert into 9000_VERINFO_PUB (BRIF_VER) values (?)");
        pst.setString(1, os_version);
        pst.close();

        String sql = "insert into 9000_VERINFO_MD5 (BRIF_VER,FILEWITHPATH,MD5_INFO) values(?,?,?)";

        for (String key : map.keySet()) {
            fw.write(key + "," + map.get(key) + "\n");
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, os_version);
            ps.setString(2, key);
            ps.setString(3, map.get(key));
            ps.executeUpdate();
        }

        stmt.close();
        conn.close();
    }
}
