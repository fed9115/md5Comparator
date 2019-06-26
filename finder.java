import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.TreeMap;

public class finder {
    private static File updated;
    private static File newArchive;
    private static File oldArchive;

    public static void main(String[] args) throws IOException {
        newArchive = new File(args[0]);
        oldArchive = new File(args[1]);
        File saveFolder = new File(args[2]);

        saveFolder.mkdirs();

        File differenceDesc = new File(args[2] + "difference.desc");
        differenceDesc.createNewFile();

        updated = new File(args[2] + "updated/");
        updated.mkdirs();

        TreeMap<String, String> mapN = new TreeMap<>();

        // Store data to the map
        BufferedReader newR = new BufferedReader(new FileReader(newArchive));
        String lineNew;
        while ((lineNew = newR.readLine()) != null) {
            String[] components = lineNew.split(",");
            mapN.put(components[0], components[1]);
        }

        // Check the old archive
        BufferedReader oldR = new BufferedReader(new FileReader(oldArchive));
        String lineOld;

        FileWriter fw = new FileWriter(differenceDesc);

        while ((lineOld = oldR.readLine()) != null) {
            String[] components = lineOld.split(",");
            if (mapN.containsKey(components[0])) {
                if (!mapN.get(components[0]).equals(components[1])) {
                    fw.write(components[0] + "," + mapN.get(components[0]) + "\n");
                    copyfile(components[0]);
                }
                mapN.remove(components[0]);
            }
        }

        // Handle the remaining items in the map which are newly added elements
        for (String key : mapN.keySet()) {
            fw.write(key + "," + mapN.get(key) + "\n");
            copyfile(key);
        }

        fw.close();

        Runtime run = Runtime.getRuntime();
        String cmd = "tar -zcvf updated.tar.gz " + updated.getPath();
        run.exec(cmd);
    }

    private static void copyfile(String path) throws IOException {
        String[] dirs = path.split("/");
        String nameFull = newArchive.getName();
        String nameActual = nameFull.substring(0, nameFull.indexOf("desc") - 1);
        String multilevel = "/";
        for (int i = 0; i < dirs.length; ++i) {
            if (dirs[i].equals(nameActual)) {
                for (int j = i + 1; j < dirs.length - 1; ++j) {
                    multilevel += dirs[j] + "/";
                }
                break;
            }
        }

        File wrapDirs = new File(updated.getPath() + multilevel);
        wrapDirs.mkdirs();
        File exist = new File(path);
        File newFile = new File(wrapDirs.getPath() + "/" + exist.getName());
        Files.copy(exist.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
