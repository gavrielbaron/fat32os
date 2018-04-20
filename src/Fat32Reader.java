/**
 * @author      Jonah Taurog and Gavriel Baron
 * @version     1.0
 * @date        4/15/2018
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;

public class Fat32Reader {
    private int BPB_BytsPerSec, BPB_SecPerClus, BPB_RsvdSecCnt, BPB_NumFATs, BPB_FATSz32, bytesPerCluster, BPB_RootClus;
    private int BPB_RootEntCnt, RootDirSectors, FirstDataSector, FATOffSet, FatSecNum, FATEntOffset;
    private int N, FirstSectorofRootCluster, FatTableStart;
    private Directory root;
    private Directory currentDir;
    private String currentDirName = "";
    private HashSet<String> name = new HashSet<>();
    private byte[] data;

    public static void main(String[] args) throws IOException {
        Fat32Reader f = new Fat32Reader();
        //Start up the calculations for this fileSystem including the BPBs and the characteristics
        // of each file.
        Path p = Paths.get(args[0]);
        f.data = Files.readAllBytes(p);
        f.initiate();
        String commandLine;
        while(true){
            System.out.print("/" + f.currentDirName + "]");
            Scanner scanner = new Scanner(System.in);
            String s = scanner.nextLine();
            String[] input = s.split(" ");
            commandLine = input[0];
            if(commandLine.equalsIgnoreCase("info")){
                f.info();
            }
            else if(commandLine.equalsIgnoreCase("stat")){
                try {
                    f.stat(input[1]);
                }
                catch (ArrayIndexOutOfBoundsException e){
                    System.out.println("Input a file!");
                }
            }
            else if(commandLine.equalsIgnoreCase("size")){
                try{
                    f.size(input[1]);
                }catch (ArrayIndexOutOfBoundsException e){
                    System.out.println("Input a file!");
                }
            }
            else if(commandLine.equalsIgnoreCase("cd")){
                try{
                    f.cd(input[1]);
                }catch (ArrayIndexOutOfBoundsException e){
                    System.out.println("Input a file!");
                }
            }
            else if(commandLine.equalsIgnoreCase("ls")){
                f.ls();
            }
            else if(commandLine.equalsIgnoreCase("read")){
                try{
                    f.read(input[1], input[2], input[3]);
                }catch (ArrayIndexOutOfBoundsException e){
                    System.out.println("Input a file and lower and upper bounds! (3 args)");
                }
            }
            else if(commandLine.equalsIgnoreCase("volume")){
                //System.out.print("Going to volume!\n");
                f.volume();
            }
            else if(commandLine.equalsIgnoreCase("quit")){
                System.out.print("Quitting..\n");
                System.exit(0);

            }
            else{
                System.out.print("Unrecognized command!\n");
            }
        }
    }
    /**
     * Makes all the necessary BPB calculations to do the commands
     */
    public void initiate() {
        BPB_BytsPerSec = getBytes(11,2);
        BPB_SecPerClus = getBytes(13,1);
        BPB_RsvdSecCnt = getBytes(14,2);
        BPB_NumFATs = getBytes(16,1);
        BPB_FATSz32 = getBytes(36,4);
        BPB_RootClus = getBytes( 44, 4);
        BPB_RootEntCnt = getBytes( 17, 2);
        RootDirSectors = ((BPB_RootEntCnt * 32) + (BPB_BytsPerSec - 1)) / BPB_BytsPerSec;
        FirstDataSector = BPB_RsvdSecCnt + (BPB_NumFATs * BPB_FATSz32) + RootDirSectors;
        FirstSectorofRootCluster = ((BPB_RootClus - 2) * BPB_SecPerClus) + FirstDataSector;
        updateDirList();
    }
    /**
     * This method uses the first sector of the root directory cluster and scans the drive for as long as the cluster and
     * puts all the file names and extension bytes into a new array. That array is then converted to a string
     * and cleaned up to look all neat and pretty with the proper spaces and punctuation. We are only dealing with
     * short files which are 11 bytes long and there are 64 bytes between each file name.
     * This method also gathers the info about each file in the root and puts each file Object (Directory) in an array
     */
    public void updateDirList() {
        int startOfRootDirectory = FirstSectorofRootCluster * BPB_BytsPerSec;
        bytesPerCluster = BPB_BytsPerSec * BPB_SecPerClus;
        for(int i = startOfRootDirectory; i < startOfRootDirectory + bytesPerCluster; i+=64){
            int dirAttribute = getBytes(i + 11,1);
            int size = getBytes(i + 28, 4);
            String low = Integer.toHexString(getBytes(i + 26, 2));
            String hi = Integer.toHexString(getBytes(i + 20, 2));
            String currentName = getStringFromBytes( i, 11);
            if (!currentName.contains("\u0000")){
                String finalName = Normalizer.normalize(makeNamePretty(currentName, dirAttribute), Normalizer.Form.NFD);
                if (dirAttribute == 8) {
                    root = new Directory(finalName, dirAttribute, size, low, hi, null);
                    currentDir = root;
                } else {
                    root.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, root));
                }
            }
        }
    }
    /**
     * This method fixes up the file name making sure to add the "." and erase any empty space.
     * every 8th index there should be a dot which comes before the extension.
     */
    public String makeNamePretty(String currentName, int dirAttribute){
        if(dirAttribute == 8) return currentName;
        String prettyString = "";
        int extensionControl = 0;
        for (int k = 0; k < currentName.length(); k++) {
            if(currentName.charAt(k) != ' ' && extensionControl != 7) {
                prettyString += Character.toString(currentName.charAt(k));
            } else if(extensionControl == 7){
                if(currentName.charAt(k + 1) == ' '){
                    prettyString += " ";
                } else if (currentName.charAt(k) != ' ') {
                    prettyString += Character.toString(currentName.charAt(k)) + ".";
                } else {
                    prettyString += ".";
                }
            }
            extensionControl++;
        }
        String finalResult = prettyString.toLowerCase().replaceAll(" ", "");
        return finalResult;
    }
    /**
     * This method reads through the bytes and determines the bytes
     * in the correct endian-ness given an offset and a size
     */
    public int getBytes(int offset, int size) {
        String hex = "";
        for(int i = offset + size - 1; i >= offset; i--){
            String temp = Integer.toHexString(data[i] & 0xFF);
            if(Integer.parseInt(temp, 16) <= 16) {
                hex += "0" + temp;
            } else hex += temp;
        }
        int result = Integer.parseInt(hex, 16);
        return result;
    }
    /**
     * This method reads through the bytes and determines the resulting string given when reading
     * the bytes in the proper endian-ness
     */
    public String getStringFromBytes(int offset, int size) {
        byte[] newData = new byte[size];
        int j = size - 1;
        for(int i = offset + size - 1; i >= offset; i--){
            newData[j] = data[i];
            j--;
        }
        String s = new String(newData);
        Normalizer.normalize(s, Normalizer.Form.NFD);
        return s;
    }
    public int firstClusterNumber(Directory dir){
        String clusterNumStr = dir.getHigh() + dir.getLow();
        int clusterNum = Integer.parseInt(clusterNumStr, 16);
        return clusterNum;
    }
    /**
     * Prints out the info of certain BPBs in boot sector
     */
    public void info() {
        System.out.println("BPB_BytsPerSec: 0x" +Integer.toHexString(BPB_BytsPerSec) + ", " + BPB_BytsPerSec +
                "\nBPB_SecPerClus" + ": 0x" + Integer.toHexString(BPB_SecPerClus) + ", " +BPB_SecPerClus +
                "\nBPB_RsvdSecCnt: 0x" + Integer.toHexString(BPB_RsvdSecCnt) + ", " + BPB_RsvdSecCnt +
                "\nBPB_NumFATs: 0x" + Integer.toHexString(BPB_NumFATs) + ", " +BPB_NumFATs +
                "\nBPB_FATSz32: 0x" + Integer.toHexString(BPB_FATSz32) + ", " + BPB_FATSz32);
    }
    /**
     * Prints out each file name unless it's the "free" file, the volume Id (i.e. the root) or a hidden file.
     */
    public void ls() {
        for (Directory directory : currentDir.getChildren()) {
            int attr = directory.getDirAttribute();
            if (attr != 8 && directory.getName().charAt(0) != (char) 65533 && attr != 2) {
                System.out.print(directory.getName() + "   ");
            }
        }
        System.out.println();
    }
    /**
     *Prints out the information about the file. First checks if the argument even exists and if so, print out the size,
     * attributes and the cluster number.
     */
    public void stat(String fileName){
        for (Directory dir : currentDir.getChildren()){
            if(dir.getName().equalsIgnoreCase(fileName)) {
                System.out.println("Size is " + dir.getSize());
                System.out.print("Attributes ");
                switch(dir.getDirAttribute()){
                    case 1: System.out.println("ATTR_READ_ONLY");
                    case 2: System.out.println("ATTR_HIDDEN");
                    case 4: System.out.println("ATTR_SYSTEM ");
                    case 8: System.out.println("ATTR_VOLUME_ID ");
                    case 16: System.out.println("ATTR_DIRECTORY");
                    case 32: System.out.println("ATTR_ARCHIVE ");
                }
                int clusterNum = firstClusterNumber(dir);
                System.out.println("Next cluster number is 0x" + clusterNum);
                return;
            }
        }
        System.out.println("Error: file/directory does not exist");
    }
    public void volume(){
        if(root.getName() != null) System.out.println(root.getName());
        else System.out.println("Error: volume name not found!");
    }
    public void size(String fileName){
        for (Directory dir : currentDir.getChildren()){
            if(dir.getName().equalsIgnoreCase(fileName)) {
                System.out.println("Size is " + dir.getSize());
                return;
            }
        }
        System.out.println("Error: file not found!");
    }
    public void cd(String fileName){
        if(fileName.equals(".")) return;
        else if(fileName.equals("..")){
            if(currentDir == root){
                System.out.println("Already at the root!"); return;
            }
            currentDir = currentDir.getParent();
            if(currentDir == root) {
                currentDirName = "";
            } else currentDirName = currentDir.getName() + "/"; return;
        }
        else{
            for(Directory dir : currentDir.getChildren()){
                if(fileName.equals(dir.getName()) && dir.isFile()){
                    System.out.println("Error: not a directory"); return;
                }
                else if(fileName.equals(dir.getName()) && !dir.isFile()){
                    if (name.add(fileName)) {
                        getNewFileInfo(dir);
                        Collections.sort(currentDir.getChildren());
                    } else currentDir = dir;
                    currentDirName = fileName + "/";
                    return;
                }
            }
            System.out.println("Error: does not exist!");
        }
    }
    public void getNewFileInfo(Directory dir){
        currentDir = dir;
        ArrayList<Integer> dirStarts = new ArrayList<>();
        N = firstClusterNumber(dir);
        getDirStarts(dirStarts, N);
        for(int k = 0; k < dirStarts.size(); k++) {
            int j = 0;
            int startOfDir = dirStarts.get(k);
            if(k <= 0) {
                for (int i = startOfDir; i < startOfDir + bytesPerCluster; i += 64) {
                    int dirAttribute = getBytes( i + 11, 1);
                    int size = getBytes(i + 28, 4);
                    String low = Integer.toHexString(getBytes( i + 26, 2));
                    String hi = Integer.toHexString(getBytes(i + 20, 2));
                    String currentName = getStringFromBytes(i, 11);
                    if (!currentName.contains("\u0000")) {
                        String finalName = Normalizer.normalize(makeNamePretty(currentName, dirAttribute), Normalizer.Form.NFD);
                        currentDir.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, currentDir));
                    }
                    if (j++ < 1) i -= 32;
                }
            } else{
                for (int i = startOfDir + 32; i < startOfDir + bytesPerCluster; i += 64) {
                    int dirAttribute = getBytes( i + 11, 1);
                    int size = getBytes(i + 28, 4);
                    String low = Integer.toHexString(getBytes(i + 26, 2));
                    String hi = Integer.toHexString(getBytes(i + 20, 2));
                    String currentName = getStringFromBytes(i, 11);
                    if (!currentName.contains("\u0000")) {
                        String finalName = Normalizer.normalize(makeNamePretty(currentName, dirAttribute), Normalizer.Form.NFD);
                        currentDir.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, currentDir));
                    }
                }
            }
        }
    }
    public void getDirStarts(ArrayList<Integer> a, int next){
        N = next;
        FATOffSet = N * 4;
        FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
        FatTableStart = FatSecNum * BPB_BytsPerSec;
        FATEntOffset = FATOffSet % BPB_BytsPerSec;
        int clusterOffset = FATEntOffset + FatTableStart;
        int nextClus = getBytes(clusterOffset, 4);
        int firstSectorofDirCluster = ((N - 2) * BPB_SecPerClus) + FirstDataSector;
        int startOfDir = firstSectorofDirCluster * BPB_BytsPerSec;
        a.add(startOfDir);
        if(nextClus <= 268435447) {
            getDirStarts(a, nextClus);
        }
    }
    public void read(String fileName, String lower, String upper){
        for(Directory dir : currentDir.getChildren()){
            if(fileName.equals(dir.getName()) && !dir.isFile()){
                System.out.println("Error: not a File!"); return;
            }
            else if(fileName.equals(dir.getName()) && dir.isFile()){
                read(dir, lower, upper);
                return;
            }
        }
        System.out.println("Error: does not exist!");
    }
    public void read(Directory dir, String lower, String upper){
        int lo = Integer.parseInt(lower);
        int hi = Integer.parseInt(upper);
        if (hi - lo > dir.getSize() || lo < 0 || hi < 0 || lo > hi) {
            System.out.println("Error: attempt to read beyond end of file");
            return;
        }
        N = firstClusterNumber(dir);
        FATOffSet = N * 4;
        int firstSectorofDirCluster = ((N - 2) * BPB_SecPerClus) + FirstDataSector;
        int startOfDir = firstSectorofDirCluster * BPB_BytsPerSec;
        String s = getStringFromBytes(startOfDir + lo, hi );
        char k = s.charAt(s.length() - 1);
        String finalString = s.replaceAll("\u0000", "");
        System.out.println(finalString);
    }
}

