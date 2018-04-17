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
import java.util.ArrayList;
import java.util.Scanner;


public class Fat32Reader {
    private int BPB_BytsPerSec, BPB_SecPerClus, BPB_RsvdSecCnt, BPB_NumFATs, BPB_FATSz32;
    private int BPB_RootClus, BPB_RootEntCnt, RootDirSectors, FirstDataSector, FirstSectorofCluster;
    private ArrayList<Directory> dirList = new ArrayList<Directory>();

    public static void main(String[] args) throws IOException {
        Fat32Reader f = new Fat32Reader();
        //Start up the calculations for this fileSystem including the BPBs and the characteristics
        // of each file.
        f.initiate(args[0]);
        String commandLine = "";

        while(true){

            System.out.print("/]");
            Scanner scanner = new Scanner(System.in);
            String s = scanner.nextLine();
            String[] input = s.split(" ");
            commandLine = input[0];


            if(commandLine.equalsIgnoreCase("info")){
                f.info(args[0]);
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
                System.out.print("Going to size!\n");
                try{
                	f.size(input[1]);
                }catch (ArrayIndexOutOfBoundsException e){
                	System.out.println("Input a file!");
                }
            }
            else if(commandLine.equalsIgnoreCase("cd")){
                System.out.print("Going to cd!\n");
            }
            else if(commandLine.equalsIgnoreCase("ls")){
                f.ls("");
            }
            else if(commandLine.equalsIgnoreCase("read")){
                System.out.print("Going to read!\n");
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
    public void initiate(String path) throws IOException{
        BPB_BytsPerSec = getBytes(path,11,2);
        BPB_SecPerClus = getBytes(path,13,1);
        BPB_RsvdSecCnt = getBytes(path,14,2);
        BPB_NumFATs = getBytes(path,16,1);
        BPB_FATSz32 = getBytes(path,36,4);
        BPB_RootClus = getBytes(path, 44, 4);
        BPB_RootEntCnt = getBytes(path, 17, 2);
        RootDirSectors = ((BPB_RootEntCnt * 32) + (BPB_BytsPerSec - 1)) / BPB_BytsPerSec;
        FirstDataSector = BPB_RsvdSecCnt + (BPB_NumFATs * BPB_FATSz32) + RootDirSectors;
        FirstSectorofCluster = ((BPB_RootClus - 2) * BPB_SecPerClus) + FirstDataSector;
        updateDirList(path);
    }
    /**
     * This method uses the first sector of the root directory cluster and scans the drive for as long as the cluster and
     * puts all the file names and extension bytes into a new array. That array is then converted to a string
     * and cleaned up to look all neat and pretty with the proper spaces and punctuation. We are only dealing with
     * short files which are 11 bytes long and there are 64 bytes between each file name.
     * This method also gathers the info about each file in the root and puts each file Object (Directory) in an array
     */
    public void updateDirList(String path) throws IOException{
        int startOfRootDirectory = FirstSectorofCluster * BPB_BytsPerSec;
        Path p = Paths.get(path);
        byte[] data = Files.readAllBytes(p);
        int bytesPerCluster = BPB_BytsPerSec * BPB_SecPerClus;
        byte[] file = new byte[bytesPerCluster];
        int currentFile = 0;

        int j = 0;
        String s = "";
        for(int i = startOfRootDirectory; i < startOfRootDirectory + bytesPerCluster; i+=64){
            j++;
            int dirAttribute = getBytes(path, i + 11,1);
            int size = getBytes(path, i + 28, 4);
            String low = Integer.toHexString(getBytes(path, i + 26, 2));
            String hi = Integer.toHexString(getBytes(path, i + 20, 2));
            String currentName = getStringFromBytes(path, i, 11);
            if (currentName.contains("\u0000")){
                break;
            }
            String finalName = Normalizer.normalize(makeNamePretty(currentName, dirAttribute), Normalizer.Form.NFD);



            dirList.add(new Directory(finalName, dirAttribute, size, low, hi));
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
    public int getBytes(String path, int offset, int size) throws IOException{
            Path p = Paths.get(path);
            byte[] data = Files.readAllBytes(p);
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
    public String getStringFromBytes(String path, int offset, int size) throws IOException{
        Path p = Paths.get(path);
        byte[] allData = Files.readAllBytes(p);
        byte[] data = new byte[size];
        int j = size - 1;
        for(int i = offset + size - 1; i >= offset; i--){
            data[j] = allData[i];
            j--;
        }
        String s = new String(data);
        Normalizer.normalize(s, Normalizer.Form.NFD);
        return s;
    }

    /**
     * Prints out the info of certain BPBs in boot sector
     */
    public void info(String path) throws IOException{
        System.out.println("BPB_BytsPerSec: 0x" +Integer.toHexString(BPB_BytsPerSec) + ", " + BPB_BytsPerSec +
                "\nBPB_SecPerClus" + ": 0x" + Integer.toHexString(BPB_SecPerClus) + ", " +BPB_SecPerClus +
                "\nBPB_RsvdSecCnt: 0x" + Integer.toHexString(BPB_RsvdSecCnt) + ", " + BPB_RsvdSecCnt +
                "\nBPB_NumFATs: 0x" + Integer.toHexString(BPB_NumFATs) + ", " +BPB_NumFATs +
                "\nBPB_FATSz32: 0x" + Integer.toHexString(BPB_FATSz32) + ", " + BPB_FATSz32);
    }
    /**
     * Prints out each file name unless it's the "free" file, the volume Id (i.e. the root) or a hidden file.
     */
    public void ls(String path) throws IOException{
    for (Directory directory : dirList){
        int attr = directory.getDirAttribute();
        if(attr != 8 && directory.getName().charAt(0) != (char)65533 && attr != 2)  {
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
        for (Directory dir : dirList){
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
                String clusterNumStr = dir.getHigh() + dir.getLow();
                int clusterNum = Integer.parseInt(clusterNumStr, 16);
                System.out.println("Next cluster number is 0x" + clusterNum);
                return;
            }
        }
        System.out.println("Error: file/directory does not exist");

    }

    public void volume(){
        for (Directory dir : dirList){
            if(dir.getDirAttribute() == 8){
                System.out.println(dir.getName());
                return;
            }
        }
        System.out.println("Error: volume name not found!");
    }

    public void size(String fileName){
    	for (Directory dir : dirList){
            if(dir.getName().equalsIgnoreCase(fileName)) {
                System.out.println("Size is " + dir.getSize());
                return;
            }
    	}
    	System.out.println("Error: file not found!");
    }



}
