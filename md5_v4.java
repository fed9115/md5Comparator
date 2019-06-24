package md5_v4;

import org.apache.commons.codec.digest.DigestUtils;

import cs1410lib.Dialogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * This file is to automatically generate the MD5 codes for each file in each directory from the two folders passed as
 * the parameters, and then pack all different items in the two folders by their directories to the save path.
 *
 * @author Erdi Fan
 */

public class md5_v4 {

    private static TreeSet<String> set_c = new TreeSet<>();
    private static TreeSet<String> set_n = new TreeSet<>();
    private static TreeSet<String> set_o = new TreeSet<>();

    public static void main(String[] args) throws IOException {
        String newerPath = Dialogs.showInputDialog("Please press the path of the new vesion folder") + "/";
        String olderPath = Dialogs.showInputDialog("Please press the path of the old vesion folder") + "/";
        String savepathInitial = Dialogs.showInputDialog("Please press the path of the folder you would like to save into") + "/";
        String saveFolderName = getSaveFolderName(savepathInitial);

        // Prepare the save folder
        File saveFolder = new File(savepathInitial);
        saveFolder.mkdirs();

        // the first argument is the one for the newer version folder
        // two versions folders
        File newer = new File(newerPath);
        // the second argument is the one for the older version folder
        File older = new File(olderPath);

        // Storing directories into 2 array lists
        ArrayList<File> dirList1 = new ArrayList<>(Arrays.asList(newer.listFiles()));
        ArrayList<File> dirList2 = new ArrayList<>(Arrays.asList(older.listFiles()));

        // Iterate two directory lists to get the directories with same name
        for (int i = 0; i < dirList1.size(); ++i) {
            if (dirList1.get(i).isDirectory()) {
                String dirname1 = dirList1.get(i).getName();
                for (int j = 0; j < dirList2.size(); ++j) {
                    if (dirList2.get(j).isDirectory()) {
                        String dirname2 = dirList2.get(j).getName();
                        if (dirname1.equals(dirname2)) {
                            ArrayList<File> progFileList1 = new ArrayList<>(Arrays.asList(dirList1.get(i).listFiles()));
                            ArrayList<File> progFileList2 = new ArrayList<>(Arrays.asList(dirList2.get(j).listFiles()));

                            // Check the md5 codes
                            checkFileMd5(savepathInitial, dirname1, progFileList1, progFileList2);
                            // Remove the directory from two dirLists and then redirect the pointers
                            dirList1.remove(i);
                            dirList2.remove(j);
                            --i;
                            --j;
                            break;
                        }
                    }
                }
            }
        }

        // Handle the remaining directories
        if (!dirList1.isEmpty()) {
            remainDirHandler(savepathInitial, dirList1, set_n);
        }
        if (!dirList2.isEmpty()) {
            remainDirHandler(savepathInitial, dirList2, set_o);
        }


        // Create 3 .txt files to store the abstract path info for the items which are different in the 2 versions
        generateTxts(savepathInitial + "changed.txt", set_c);
        generateTxts(savepathInitial + "new_added.txt", set_n);
        generateTxts(savepathInitial + "old_deleted.txt", set_o);

        deleteEmpty(savepathInitial);

        Runtime run = Runtime.getRuntime();
        String cmd = "tar -zcvf " + saveFolderName + ".tar.gz " + savepathInitial;
        run.exec(cmd);
    }

	/**
     * Check the md5 codes from the 2 progFileLists: if same, write down in the archive files and skip, if different,
     * put them in the update package
     *
     * @param dirName
     * @param progFileList1
     * @param progFileList2
     * @throws IOException
     */
    private static void checkFileMd5(String path, String dirName, ArrayList<File> progFileList1,
                                     ArrayList<File> progFileList2) throws IOException {
        // Iterate the 2 progFileLists to find the files with the same md5 code, writing down the code into two
        // archive file and removing them from the list
        for (int i = 0; i < progFileList1.size(); ++i) {
            if (!progFileList1.get(i).isDirectory()) {
                // Get the md5 code for current program file in the currenct directory from newer version folder
                FileInputStream fis1 = new FileInputStream(progFileList1.get(i));
                String md5_1 = DigestUtils.md5Hex(fis1);
                fis1.close();
                for (int j = 0; j < progFileList2.size(); ++j) {
                    if (!progFileList2.get(j).isDirectory()) {
                        FileInputStream fis2 = new FileInputStream(progFileList2.get(j));
                        String md5_2 = DigestUtils.md5Hex(fis2);
                        fis2.close();
                        if (md5_1.equals(md5_2)) {
                            // Remove the files from two progFileLists and then redirect the pointers
                            progFileList1.remove(i);
                            progFileList2.remove(j);
                            --i;
                            --j;
                            break;
                        }
                    }
                }
            }
        }
        File currDir = new File(path + "/" + dirName);
        currDir.mkdirs();
        checkEditedItems(currDir.getPath(), progFileList1, progFileList2);

        writeUpdate(currDir.getPath(), progFileList1, set_n);
        writeUpdate(currDir.getPath(), progFileList2, set_o);
    }

    /**
     * check the duplicated items which are edited from the old version in the new version. The two files should have
     * the same name
     *
     * @param path
     * @param progFileList1
     * @param progFileList2
     * @throws IOException
     */
    private static void checkEditedItems(String path, ArrayList<File> progFileList1, ArrayList<File> progFileList2) throws IOException {
        for (int i = 0; i < progFileList1.size(); ++i) {
            if (!progFileList1.get(i).isDirectory()) {
                String name1 = progFileList1.get(i).getName();
                for (int j = 0; j < progFileList2.size(); ++j) {
                    if (!progFileList2.get(j).isDirectory()) {
                        String name2 = progFileList2.get(j).getName();
                        if (name1.equals(name2)) {
                            // that two files should have the same file name but different md5 codes: store the copy
                            // of the
                            // file in the newer version under the current directory, write down the abstract path in
                            // changed
                            // .txt
                            File newFile = new File(path + "/" + name1);
                            File exist = new File(progFileList1.get(i).getPath());
                            Files.copy(exist.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            addToSet(progFileList1.get(i).getAbsolutePath(), set_c);

                            progFileList1.remove(i);
                            progFileList2.remove(j);
                            --i;
                            --j;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * This helper method is designed for handling all the remaining directories which are not matched from the newer
     * version and the older version: just get all the files in those directories and then write down their md5 codes
     * and store them in the update package
     *
     * @param path
     * @param dirList
     * @param set
     * @throws IOException
     */
    private static void remainDirHandler(String path, ArrayList<File> dirList, TreeSet<String> set) throws IOException {
        for (File dir : dirList) {
            if (dir.isDirectory()) {
                ArrayList<File> fileList = new ArrayList<>(Arrays.asList(dir.listFiles()));
                if (set.equals(set_n)) {
                    File newDir = new File(path + "/" + dir.getName());
                    newDir.mkdirs();
                    writeUpdate(newDir.getPath(), fileList, set);
                } else {
                    writeUpdate(path, fileList, set);
                }
            }
        }
    }

    /**
     * Helper method to put files into the update package and handle the md5 codes of the files which are different
     * in the lists
     *
     * @param path
     * @param fileList
     * @param set
     * @throws IOException
     */
    private static void writeUpdate(String path, ArrayList<File> fileList, TreeSet<String> set) throws IOException {
        if (!fileList.isEmpty()) {
            // After removing those files, the remaining files in the directory are the ones which are newly added or
            // deleted

            // if the fw is fw_n, that means in the fileList, all the items are newly added, we need to write down
            // their path to the txt file as well as add their copies to the specific directory
            if (set.equals(set_n)) {
                for (int i = 0; i < fileList.size(); ++i) {
                    if (!fileList.get(i).isDirectory()) {
                        addToSet(fileList.get(i).getAbsolutePath(), set);
                        File newAdded = new File(path + "/" + fileList.get(i).getName());
                        File exist = new File(fileList.get(i).getPath());
                        Files.copy(exist.toPath(), newAdded.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } else {
                // if the fw is not fw_n, do not put the copies into the directory
                for (int i = 0; i < fileList.size(); ++i) {
                    if (!fileList.get(i).isDirectory()) {
                        addToSet(fileList.get(i).getAbsolutePath(), set);
                    }
                }
            }
        }
    }

    /**
     * Helper method to avoid writing down soft links in the txt files and add it to the set
     *
     * @param currPath
     */
    private static void addToSet(String currPath, TreeSet<String> set) {
        int i = currPath.length() - 1;
        // Check the last character in the String which is not a digit: which means we only need to keep the
        // components before it
        while (i >= 0 && Character.isDigit(currPath.charAt(i))) {
            i -= 2;
        }
        set.add(currPath.substring(0, i + 1));
    }


    private static void generateTxts(String pathName, TreeSet<String> set) throws IOException {
        File file = new File(pathName);
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        int num = set.size();
        writeToTxt(fw, set);
        fw.write("一共有 " + num + " 个文件");
        fw.close();
    }

    /**
     * Helper method to generate components in txt files
     *
     * @param fw
     * @param set
     */
    private static void writeToTxt(FileWriter fw, TreeSet<String> set) throws IOException {
        for (String str : set) {
            fw.write(str);
            fw.write("\n");
        }
    }

    /**
     * Handle the empty folders and files in the save folder
     *
     * @param savepathInitial
     */
    private static void deleteEmpty(String savepathInitial) {
        File lagrgestFolder = new File(savepathInitial);
        for (File f : lagrgestFolder.listFiles()) {
            if (f.length() == 0) {
                f.delete();
                break;
            }

            if (f.isDirectory() && f.listFiles().length == 0) f.delete();
        }
    }
    

    private static String getSaveFolderName(String savepathInitial) {
    	int i = savepathInitial.length() - 2;
        // Check the last character in the String which is not a digit: which means we only need to keep the
        // components before it
        while (i >= 0 && savepathInitial.charAt(i) != '/') {
            --i;
        }
        return savepathInitial.substring(i + 1, savepathInitial.length() - 1);
	}
}