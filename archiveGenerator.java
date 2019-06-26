import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.util.TreeMap;
import java.util.TreeSet;

public class archiveGenerator {
    // two tree maps
    private static TreeMap<String, String> map = new TreeMap<>();
    private static TreeSet<File> ignoredFiles = new TreeSet<>();

    public static void main(String[] args) throws IOException {
        String path = args[0];
        String savepathInitial = args[1];

        if (args.length == 3) {
            File configuration = new File(args[2]);
            FilesToIgnore(configuration, ignoredFiles);
        }

        String versionName = getFolderName(path);

        // Prepare the save folder
        File saveFolder = new File(savepathInitial);
        saveFolder.mkdirs();

        // the first argument is the one for the version folder
        File version = new File(path);

        File[] newArr = version.listFiles();


        recursiveFinding(newArr, 0, map);

        FileWriter fw1 = new FileWriter(new File(savepathInitial + versionName + ".desc"));
        writeArchive(fw1, map);
        fw1.close();
    }

    private static void FilesToIgnore(File configuration, TreeSet<File> ignoredFiles) throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(configuration));
        String line;
        while ((line = bf.readLine()) != null) {
            File f = new File(line);
            ignoredFiles.add(f);
        }
    }

    private static void recursiveFinding(File[] arr, int index, TreeMap<String, String> map) throws IOException {
        if (index == arr.length) return;
        if (arr[index].isFile() && !ignoredFiles.contains(arr[index])) {
            FileInputStream fis = new FileInputStream(arr[index]);
            String currMd5 = DigestUtils.md5Hex(fis);
            fis.close();
            addToMap(arr[index].getPath(), currMd5, map);
        } else if (arr[index].isDirectory() && !ignoredFiles.contains(arr[index])) {
            // recursion for sub-directories
            recursiveFinding(arr[index].listFiles(), 0, map);
        }
        // recursion for main directory
        recursiveFinding(arr, ++index, map);
    }

    private static String getFolderName(String savepathInitial) {
        int i = savepathInitial.length() - 2;
        // Check the last character in the String which is not a digit: which means we only need to keep the
        // components before it
        while (i >= 0 && savepathInitial.charAt(i) != '/') {
            --i;
        }
        return savepathInitial.substring(i + 1, savepathInitial.length() - 1);
    }

    /**
     * Helper method to avoid writing down soft links in the archive files: add it to the map
     *
     * @param currPath
     */
    private static void addToMap(String currPath, String md5, TreeMap<String, String> map) {
        int i = currPath.length() - 1;
        // Check the last character in the String which is not a digit: which means we only need to keep the
        // components before it
        while (i >= 0 && Character.isDigit(currPath.charAt(i))) {
            i -= 2;
        }
        String rearrangedPath = currPath.substring(0, i + 1);

        map.put(rearrangedPath, md5);
    }

    private static void writeArchive(FileWriter fw, TreeMap<String, String> map) throws IOException {
        for (String key : map.keySet()) {
            fw.write(key + "," + map.get(key) + "\n");
        }
    }
}
