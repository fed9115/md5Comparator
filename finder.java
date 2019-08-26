package find;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.TreeMap;

public class finder {
    private static File updated;
    private static File newArchive;
    private static File oldArchive;
    private static String binFolderPath;
    private static int totCount = 0;

    public static void main(String[] args) throws IOException {
        newArchive = new File(args[0]);
        oldArchive = new File(args[1]);
        File saveFolder = new File(args[2]);
        binFolderPath = args[3];

        saveFolder.mkdirs();

        String os_new_old = fileNameGenerator(newArchive, oldArchive);

        File differenceDesc = new File(args[2] + os_new_old + ".DIFF");
        differenceDesc.createNewFile();

        updated = new File(args[2] + os_new_old + "/");
        updated.mkdirs();

        TreeMap<String, String> mapN = new TreeMap<>();

        // Store data to the map
        BufferedReader newR = new BufferedReader(new FileReader(newArchive));
        String lineNew;
        while ((lineNew = newR.readLine()) != null && lineNew.contains(",")) {
            String[] components = lineNew.split(",");
            mapN.put(components[0], components[1]);
        }
        newR.close();

        // Check the old archive
        BufferedReader oldR = new BufferedReader(new FileReader(oldArchive));

        FileWriter fw = new FileWriter(differenceDesc);

        String lineOld;

        while ((lineOld = oldR.readLine()) != null) {
            String[] components = lineOld.split(",");
            if (mapN.containsKey(components[0])) {
                if (!mapN.get(components[0]).equals(components[1])) {
                    fw.write(components[0] + "," + mapN.get(components[0]) + "\n");
                    copyfile(binFolderPath + components[0], components[0]);
                    ++totCount;
                }
                mapN.remove(components[0]);
            }
        }

        oldR.close();

        // Handle the remaining items in the map which are newly added elements
        for (String key : mapN.keySet()) {
            fw.write(key + "," + mapN.get(key) + "\n");
            copyfile(binFolderPath + key, key);
            ++totCount;
        }

        fw.write("一共有 " + totCount + " 个差异/新增项");
        fw.close();

        // generate the tar.gz package
        Runtime run = Runtime.getRuntime();
        String cmd = "tar -zcvf " + os_new_old + "_PACK.tar.gz " + updated.getPath();
        run.exec(cmd);
    }

    private static String fileNameGenerator(File newArchive, File oldArchive) {
        String str1 = newArchive.getName().substring(0, newArchive.getName().length() - 4);
        String str2 = oldArchive.getName().split("_")[1];
        return (str1 + "_" + str2).replace(".MD5", "");
    }

    private static void copyfile(String path, String relativePath) throws IOException {
        String multilevel = "/" + relativePath.substring(0, relativePath.lastIndexOf('/') + 1);

        File wrapDirs = new File(updated.getPath() + multilevel);
        wrapDirs.mkdirs();

        if (path.contains(".so")) {
            String folderPath = path.substring(0, path.lastIndexOf('/'));
            File thisFolder = new File(folderPath);
            for (File f : thisFolder.listFiles()) {
                if (f.getPath().contains(relativePath)) {
                    File newFile = new File(wrapDirs.getPath() + "/" + f.getName());
                    Files.copy(f.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
                            LinkOption.NOFOLLOW_LINKS);
                }
            }
        } else {
            File exist = new File(path);
            File newFile = new File(wrapDirs.getPath() + "/" + exist.getName());
            Files.copy(exist.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    LinkOption.NOFOLLOW_LINKS);
        }
    }
}
