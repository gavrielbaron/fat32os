/**
 * @author      Jonah Taurog and Gavriel Baron
 * @version     1.0
 * @date        4/22/2018
 */

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;

public class Fat32Reader {
    private int BPB_BytsPerSec, BPB_SecPerClus, BPB_RsvdSecCnt, BPB_NumFATs, BPB_FATSz32, bytesPerCluster, BPB_RootClus;
    private int BPB_RootEntCnt, RootDirSectors, FirstDataSector, FATOffSet, FatSecNum, FATEntOffset;
    private int N, FirstSectorofRootCluster, FatTableStart, endOfFATOffset;
    private Directory root;
    private Directory currentDir;
    private String currentDirName = "";
    private HashSet<String> name = new HashSet<>();
    List<Integer> freeClustersList = new ArrayList<>();
    private byte[] data;
    private byte[] FAT;
    Path p;

    public static void main(String[] args) throws IOException {
        Fat32Reader f = new Fat32Reader();
        //Start up the calculations for this fileSystem including the BPBs and the characteristics
        // of each file.
        f.p = Paths.get(args[0]);
        f.data = Files.readAllBytes(f.p); // byte array of entire fat32.img
        f.initiate(); //finds and reads the root before we execute any commands. Basically like a constructor.
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
            else if(commandLine.equalsIgnoreCase("freelist")){
                List list = f.freeList();
                System.out.print("First 3 free clusters: ");
                for(int i = 0; i < 3; i++){
                    System.out.print(list.get(i) + "  ");
                }
                System.out.println("\nAmount of free clusters: " + list.size());
            }
            else if(commandLine.equalsIgnoreCase("newfile")){
                try{
                    //f.newFile(input[1], input[2]);
                    f.newFileDetails("helloworld", 512);
                }catch (ArrayIndexOutOfBoundsException e){
                    System.out.println("Input a file name and size! (2 args)");
                }
            }
            else if(commandLine.equalsIgnoreCase("delete")){
                try{
                    f.delete(input[1]);
                }catch (ArrayIndexOutOfBoundsException e){
                    System.out.println("Input a file!");
                }
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
        FATOffSet = BPB_RootClus * 4;
        FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
        FatTableStart = FatSecNum * BPB_BytsPerSec;
        endOfFATOffset = (BPB_FATSz32 * BPB_BytsPerSec) + FatTableStart;
        FAT = new byte[endOfFATOffset - FatTableStart];
        int count = 0;
        for(int i = FatTableStart; i < endOfFATOffset; i++){
            FAT[count] = data[i];
            count++;
        }
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
            String currentName = getStringFromBytes(i, 11);
            if (!(currentName.contains("\u0000") && getBytes(i, 4 ) == 0)){
                String finalName = makeNamePretty(currentName, dirAttribute);
                if (dirAttribute == 8) {
                    root = new Directory(finalName, dirAttribute, size, low, hi, null, i);
                    currentDir = root;
                } else {
                    root.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, root, i));
                }
            }
            else{
                //   root.setNextFreeOffset(i);
                // root.setNextFreeCluster(2);
                break;
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
     * in the correct endian-ness given an offset and a size. An important method
     * which is the basis for the entire program.
     */
    public int getBytes(int offset, int size) {
        String hex = "";
        for(int i = offset + size - 1; i >= offset; i--){
            String temp = Integer.toHexString(data[i] & 0xFF);
            if(Integer.parseInt(temp, 16) < 16) {
                hex += "0" + temp;
            } else hex += temp;
        }
        int result = Integer.parseInt(hex, 16);
        return result;
    }

    /**
     * This method reads through the bytes and determines the resulting string given when reading
     * the bytes in the proper endian-ness.
     */
    public String getStringFromBytes(int offset, int size) {
        byte[] newData = new byte[size];
        int j = size - 1;
        for(int i = offset + size - 1; i >= offset; i--){
            newData[j] = data[i];
            j--;
        }
        String s = new String(newData); // turns byte array into string. Java's gift to humanity
        if(newData[0] == -27){
            char[] charArry = s.toCharArray();
            charArry[0] = (char)229;
            s = String.valueOf(charArry);
        }
        return s;
    }
    /**
     * An important method that uses the low and high byte of each file in order to determine the cluster
     * number.
     */
    public int firstClusterNumber(Directory dir){
        String clusterNumStr = dir.getHigh() + dir.getLow();
        int clusterNum = Integer.parseInt(clusterNumStr, 16);
        return clusterNum;
    }
    /**
     * Prints out the info of certain BPBs in boot sector.
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
            String name = directory.getName();
            if (attr != 8 && name.charAt(0) != (char)65533 && name.charAt(0) != (char)229 && attr != 2) {
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
    /**
     *prints the volume name, i.e. the root.
     */
    public void volume(){
        if(root.getDirAttribute() == 8) System.out.println(root.getName());
        else System.out.println("Error: volume name not found!");
    }
    /**
     *Prints out the information about the file. First checks if the argument even exists and if so, print out the size,
     * attributes and the cluster number.
     */
    public void size(String fileName){
        for (Directory dir : currentDir.getChildren()){
            if(dir.getName().equalsIgnoreCase(fileName)) {
                System.out.println("Size is " + dir.getSize());
                return;
            }
        }
        System.out.println("Error: file not found!");
    }
    /**
     *The first method regarding cd which merely parses the command and makes sure the file exists.
     */
    public void cd(String fileName){
        if(fileName.equals(".")) return;
        else if(fileName.equals("..")){
            if(currentDir == root){
                System.out.println("Already at the root!"); return;
            }
            currentDir = currentDir.getParent(); // step back a dir.
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
                    //Hashset containing all the names. if we already cd'ed into the file, no reason to re-perform all the calculations
                    if (name.add(fileName)) {
                        getNewFileInfo(dir);
                        currentDir = dir;
                        Collections.sort(currentDir.getChildren());
                    } else currentDir = dir;
                    currentDirName = fileName + "/";
                    return;
                }
            }
            System.out.println("Error: does not exist!");
        }
    }
    /**
     *The main method of to read files. we first use the low and high numbers of the file to check if it spans multiple clusters.
     * If it does, we collect the beginnings of each cluster and read through it. We then collect all the information about
     * each file, similar to what was done when we read through the root. We then set the currentDir as the cd'ed dir.
     * We keep in mind that in the first cluster the "." and ".." are 32 bytes apart and then the rest of the files
     * are 64 bytes apart.
     */
    public void getNewFileInfo(Directory dir){
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
                        dir.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, dir));
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
                        dir.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, dir));
                    }
                }
            }
        }
    }/**
     *In order to check multiple cluster spans, we have to refer to the FAT table. This method collects all info about the
     * FAT table and uses the info in the FAT table to check if there's multiple clusters and put the start of each cluster
     * in an array. 268435448 = 0xF8FFFF0F which signals that no clusters continue after the current one. Otherwise we
     * recursively go into the FAT table again and get that cluster info.
     */
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

    /**
     *this read method parses the command and if the exists reads it.
     */
    public void read(String fileName, String lower, String upper){
        for(Directory dir : currentDir.getChildren()){
            if(fileName.equals(dir.getName()) && !dir.isFile()){
                System.out.println("Error: not a File!"); return;
            }
            else if(fileName.equals(dir.getName()) && dir.isFile()){
                readDir(dir, lower, upper);
                return;
            }
        }
        System.out.println("Error: does not exist!");
    }/**
     *This method executes the actual reading. All we have to do is determine the start of the file
     * using the firstClusterNumber method and then
     * use our getStringFromBytes method to read until the upper limit. Then we print it out.
     */
    public void readDir(Directory dir, String lower, String upper){
        int lo = Integer.parseInt(lower);
        int hi = Integer.parseInt(upper);
        if (hi > dir.getSize() || lo < 0 || hi < 0 || lo > hi) {
            System.out.println("Error: attempt to read beyond end of file");
            return;
        }
        N = firstClusterNumber(dir);
        FATOffSet = N * 4;
        int firstSectorofDirCluster = ((N - 2) * BPB_SecPerClus) + FirstDataSector;
        int startOfDir = firstSectorofDirCluster * BPB_BytsPerSec;
        String s = getStringFromBytes(startOfDir + lo, hi );
        String finalString = s.replaceAll("\u0000", "");
        System.out.println(finalString);
    }

    public List<Integer> freeList(){
        // int endOfFATOffset = (BPB_FATSz32 * BPB_BytsPerSec) + FatTableStart;
        //ArrayList<Integer> list = new ArrayList<>();
        int count = 0;
        int j = 0;
        for(int i = FatTableStart; i < endOfFATOffset; i+=4){
            j++;
            // check if the index is equal to 0, i.e. is empty
            if(getBytes(i, 4) == 0){
                count++;
                freeClustersList.add(j);
                if(freeClustersList.size() == 3) {
                    // System.out.println("Indexes of first 3 free clusters : " + freeClustersList);
                }
            }

        }
        //System.out.println("Amount of free clusters: " + count);
        return freeClustersList;


    }

    public void newFile(String name, String size){
        int lengthOfName = name.length() - 4;
        if(name.charAt(lengthOfName) != '.' || name.length() > 11){
            System.out.println("You need an extention!"); return;
        }
        char[] name2 = new char[11];
        int i;
        for(i = 0; i < 8; i++){
            if(i < lengthOfName) {
                name2[i] = name.charAt(i);
            } else{
                name2[i] = (char)32;
            }
        }
        for(int j = i; j < 11; j++) {
            name2[j] = name.charAt(lengthOfName + 1);
            lengthOfName++;
        }
        String newName = String.valueOf(name2);
        int s = Integer.parseInt(size);
        makeNewFile(newName, s);
    }

    //https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
    public void makeNewFile(String name, int size){
        name = name.toUpperCase();
        byte[] newFile = new byte[64];
        byte[] fileName = name.getBytes();
        for(int i = 0; i < fileName.length; i++){
            newFile[i] = fileName[i];
        }
        //newFile[27] = (byte) size;
        newFile[11] = 32; //ordinary folder
        byte[] sizeArr = ByteBuffer.allocate(4).putInt(size).array();
        int count = 27;
        for(int i = 3; i >= 0; i--){ //putting in fileArr backwards to maintain endian-ness
            newFile[count] = sizeArr[i];
            count++;
        }
        String low, high;
        int n = size / bytesPerCluster;
        if(n == 0){
            low = String.valueOf(currentDir.getChildren().get(currentDir.getChildren().size() - 1).getNextFreeOffset());
        }
        /*
        int count1 = 0;
        for(int i = currentDir.getNextFreeOffset(); i < currentDir.getNextFreeOffset() + 64; i++){
            data[i] = newFile[count1];
            count1++;
        }*/
        System.out.println();

    }
    /* so far this method gets the cluster number and correctly sets the high and low
     * I think we need to make setters for high and low, what do you think?
     * TODO make a setHigh and setLow
     */
    public void newFileDetails(String name, int size){
        List<Integer> list = freeList();
        int clusterNumber = list.get(0);
        String hex = Integer.toHexString(clusterNumber);
        //char[] temp = hex.toCharArray();
        char[] temp = hex.toCharArray();
        char[] hexes = new char[8];
        for(int i = 0; i < hexes.length; i++){
            hexes[i] = '0';
        }
        String high = "";
        String low = "";
        int i = temp.length - 1;
        int h = hexes.length - 1;
        while(i > -1){
            hexes[h] = temp[i];
            h--;
            i--;
        }
        for(i = 0; i < 4; i += 2){
            high = "" + hexes[i] + hexes[i + 1] + high;
        }
        for(i = 4; i < 8; i += 2){
            low = "" + hexes[i] + hexes[i + 1] + low;
        }
        /*
        int clusterSpan = (int) Math.ceil(size / 512);
        if (clusterSpan > 0){

            //TODO
            //span the clusters then at last cluster put eoc
        }
        else{
            //TODO
            //put eoc
        }
        */
    }

    public void delete(String name) throws IOException{
        int i = 0;
        for( ;i < currentDir.getChildren().size(); i++){
            if(name.equalsIgnoreCase(currentDir.getChildren().get(i).getName())){
                delete(currentDir.getChildren().get(i));
                data[currentDir.getChildren().get(i).getOffsetInParent()] =(byte)229;
                currentDir.getChildren().remove(i);
                Files.write(p, data);
                return;
            }
        }/*
        for(Directory dir : currentDir.getChildren()){
            if(name.equalsIgnoreCase(dir.getName())){
                delete(dir);
                data[dir.getOffsetInParent()] = (byte)229;

            }
        } */
        System.out.println("Error: File does not exist!");



    }

    private void delete(Directory dir){
        if(!dir.isFile()){
            //meaning if dir is a folder, we have to go deeper and delete them too
            if (name.add(dir.getName())) {
                getNewFileInfo(dir);
            }
            for(Directory d : dir.getChildren()){
                delete(d);

            }
            return;
        }
        N = firstClusterNumber(dir);
        deleteClusters(N);
    }
    private void deleteClusters(int N){
        FATOffSet = N * 4;
        FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
        FatTableStart = FatSecNum * BPB_BytsPerSec;
        FATEntOffset = FATOffSet % BPB_BytsPerSec;
        int clusterOffset = FATEntOffset + FatTableStart;
        int nextClus = getBytes(clusterOffset, 4);
        if(nextClus <= 268435447) {
            zeroOutBytes(clusterOffset, 4);
            deleteClusters(nextClus);
        }
        else{
            zeroOutBytes(clusterOffset, 4);
        }

    }

    public void zeroOutBytes(int offset, int size){
        for(int i = offset; i < offset + size; i++){
            // System.out.println(data[i]);
            data[i] = (byte) 0;
        }
    }


}