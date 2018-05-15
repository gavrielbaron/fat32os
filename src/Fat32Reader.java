/**
 * @author      Jonah Taurog and Gavriel Baron
 * @version     1.1
 * @date        5/14/2018
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
<<<<<<< HEAD
=======
//import java.text.Normalizer;
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
import java.text.SimpleDateFormat;
import java.util.*;

public class Fat32Reader {
    private int BPB_BytsPerSec, BPB_SecPerClus, BPB_RsvdSecCnt, BPB_NumFATs, BPB_FATSz32, bytesPerCluster, BPB_RootClus;
    private int BPB_RootEntCnt, RootDirSectors, FirstDataSector, FATOffSet, FatSecNum, FATEntOffset;
    private int N, FirstSectorofRootCluster, FatTableStart, endOfFATOffset;
    private final int FATSIZE = 516608;
    private Directory root; //the root of our file system tree
    private Directory currentDir;
    private String currentDirName = "";
    private final String newFileContents = "New File.\r\n"; //the string to be inputted over and over in new files
    private HashSet<String> nameMap = new HashSet<>();
    private List<Integer> freeClustersList = new ArrayList<>();
    private byte[] data; //the entirety of the FAT32 disk image
    private Path p;

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
                    f.newfile(input[1], input[2]);
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
        ArrayList<Integer> rootStarts = new ArrayList<>();
        getDirStarts(rootStarts, BPB_RootClus);
<<<<<<< HEAD
=======
        //int startOfRootDirectory = FirstSectorofRootCluster * BPB_BytsPerSec;
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
        bytesPerCluster = BPB_BytsPerSec * BPB_SecPerClus;
        for(int j : rootStarts) {
            for (int i = j; i < j + bytesPerCluster; i += 64) {
                int dirAttribute = getBytes(i + 11, 1);
                int size = getBytes(i + 28, 4);
                String low = Integer.toHexString(getBytes(i + 26, 2));
                String hi = Integer.toHexString(getBytes(i + 20, 2));
                String currentName = getStringFromBytes(i, 11);
                if (!(currentName.contains("\u0000") && getBytes(i, 4) == 0)) {
                    String finalName = makeNamePretty(currentName, dirAttribute);
                    if (dirAttribute == 8) {
                        root = new Directory(finalName, dirAttribute, size, "2", hi, null, i);
                        currentDir = root;
                    } else {
                        root.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, root, i));
                    }
<<<<<<< HEAD
                } 
=======
                } else {
                    //   root.setNextFreeOffset(i);
                    // root.setNextFreeCluster(2);
                    break;
                }
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
            }
        }
        Collections.sort(currentDir.getChildren());
    }


    /**
     * This method fixes up the file name making sure to add the "." and erase any empty space.
     * every 8th index there should be a dot which comes before the extension.
     */
    public String makeNamePretty(String currentName, int dirAttribute){
        if(dirAttribute == 8) return currentName;
        int l = currentName.length();
        if(currentName.charAt(l - 1) == (char)32 && currentName.charAt(l - 2) == (char)32 && currentName.charAt(l - 3) == (char)32){
            return currentName.toLowerCase().replaceAll(" ", "");
        }
        String prettyString = "";
        int extensionControl = 0;
        for (int k = 0; k < l; k++) {
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
        return Integer.parseInt(clusterNumStr, 16);
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
                System.out.println("Next cluster number is 0x" + Integer.toHexString(clusterNum));
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
                System.out.println("Already at the root!");
                return;
            }
            currentDir = currentDir.getParent(); // step back a dir.
            if(currentDir == root) {
                currentDirName = "";
            } else currentDirName = currentDir.getName() + "/";
            return;
        }
        else{
            for(Directory dir : currentDir.getChildren()){
                if(fileName.equals(dir.getName()) && dir.isFile()){
                    System.out.println("Error: not a directory");
                    return;
                }
                else if(fileName.equals(dir.getName()) && !dir.isFile()){
<<<<<<< HEAD
                    //Hashset containing all the names.
                    // if we already cd'ed into the file, no reason to re-perform all the costly calculations
=======
                    //Hashset containing all the names. if we already cd'ed into the file, no reason to re-perform all the calculations
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
                    if (nameMap.add(fileName)) {
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
     *The main method in order to read files. we first use the low and high numbers of the file to check if it spans multiple clusters.
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
                        if(currentName.charAt(0) != (char)229) {
                            String finalName = makeNamePretty(currentName, dirAttribute);
                            dir.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, dir, i));
                        }
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
                        if(currentName.charAt(0) != (char)229) {
                            String finalName =makeNamePretty(currentName, dirAttribute);
                            dir.getChildren().add(new Directory(finalName, dirAttribute, size, low, hi, dir, i));
                        }
                    }
                }
            }
        }
    }
    /**
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
            getDirStarts(a, nextClus); //recursively search the next cluster
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
    }
    /**
<<<<<<< HEAD
     * This was a complicated method, which prints out the file according to the limits given. We gather all the clusters
=======
     * This is a complicated method, which prints out the file according to the limits given. We gather all the clusters
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
     * that file is in and read from it. If our lower limit isn't until a few clusters in, then we skip the first clusters
     * until we are in the cluster we need to be in. Then we start reading and appending as many clusters as we need
     * until the higher limit. Then we print it all out.
     */
    public void readDir(Directory dir, String lower, String upper){
        int lo = Integer.parseInt(lower);
        int hi = Integer.parseInt(upper);
        if (hi >= dir.getSize() || lo < 0 || hi < 0 || lo >= hi) {
            System.out.println("Error: attempt to read beyond end of file");
            return;
        }
        ArrayList<Integer> dirStarts = new ArrayList<>();
        N = firstClusterNumber(dir);
        getDirStarts(dirStarts, N);
        StringBuilder s = new StringBuilder();
        int count = 1;
        for(int i : dirStarts) {
            int current = (count * bytesPerCluster);
            if(lo < current && hi <= current) { //both the hi and lo are in one cluster
                s.append(getStringFromBytes(i + lo, hi - lo ));
                break;
            }
            else if(lo < current && hi > current){ // it spans more thsn one cluster
                s.append(getStringFromBytes(i + lo, bytesPerCluster - lo));
                lo = 0;
                hi = hi - bytesPerCluster;
            }
            else{ //we haven't hit the right cluster yet
                lo -= bytesPerCluster;
                hi -= bytesPerCluster;
            }
            count++;
        }
        String finalString = s.toString();
        System.out.println(finalString);
    }
    /**
     *This method scans the FAT and determines the free clusters. They are considered free if the 4 bytes
     * of the index is 0. It returns a list of the all the free clusters.
     */
    public List<Integer> freeList(){
        int j = 0;
        FATOffSet = BPB_RootClus * 4;
        FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
        FatTableStart = FatSecNum * BPB_BytsPerSec;
        freeClustersList.clear();
        for(int i = FatTableStart; i < endOfFATOffset; i+=4){
            // check if the index is equal to 0, i.e. is empty
            if(getBytes(i, 4) == 0){
                freeClustersList.add(j);
            }
            j++;

        }
        return freeClustersList;


    }
    /**
     *This method which is the first method we go to upon typing the "newfile" command
     * parses the name of the new file to make it 11 bytes long.
     */
    public void newfile(String name, String size) throws IOException{
        for(Directory dir : currentDir.getChildren()){
            if(name.equals(dir.getName())){
                System.out.println(name + " is already taken! Choose a different name");
                return;
            }
        }
        int s = Integer.parseInt(size);
        if(name.contains(".")) {
            int lengthOfName = name.length() - 4;
            char[] name2 = new char[11];
            int i;
            for (i = 0; i < 8; i++) {
                if (i < lengthOfName) {
                    name2[i] = name.charAt(i);
                } else {
                    name2[i] = (char) 32;
                }
            }
            for (int j = i; j < 11; j++) {
                name2[j] = name.charAt(lengthOfName + 1);
                lengthOfName++;
            }
            String newName = String.valueOf(name2);
            makeNewFile(name, newName, s);
        }
        else{
            char[] name2 = new char[11];
            int i;
            for(i = 0; i < name.length(); i++){
                if(i == 11) break;
                name2[i] = name.charAt(i);
            }
            if(i < 11){
                for(int j = i; j < name2.length; j++){
                    name2[j] = (char)32;
                }
            }
            String newName = String.valueOf(name2);
            makeNewFile(name, newName, s);
        }
    }

    //https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
    /**
     *This next newfile method main deals with filling the 64 byte array with information about the new
     *file, including the name, the time and date bytes, and the low and high bytes.
     * Then we create a new Directory object with all this information and add it to the tree.
     * Finally, we write to the actual disk drive all the new information.
     */
    public void makeNewFile(String name, String newName, int size) throws IOException{
        updateFAT(size);

        newName = newName.toUpperCase();
        byte[] newFile = new byte[64];
        byte[] fileName = newName.getBytes();
        for(int i = 0; i < fileName.length; i++){
            newFile[i] = fileName[i];
        }
        newFile[11] = 32; //makes the file an ordinary file
        byte[] sizeArr = ByteBuffer.allocate(4).putInt(size).array();
        int count = 28;
        for(int i = 3; i >= 0; i--){ //putting in fileArr backwards to maintain endian-ness
            newFile[count] = sizeArr[i];
            count++;
        }

        String low, high;
        ArrayList<String> list = getLowHigh();
        high = list.get(0) + list.get(1);
        low = list.get(2) + list.get(3);
        int i = 0, j = 21, k = 27;
        while(i < 2){
            newFile[j] = (byte)Integer.parseInt(list.get(i), 16);
            newFile[k] = (byte)Integer.parseInt(list.get(i + 2), 16);
            i++; j--; k--;
        }

        updateTime(newFile);
        int offsetOfFileInParent = writeFile(newFile);

        Directory dir = new Directory(name, 32, size, low, high, currentDir, offsetOfFileInParent);
        currentDir.getChildren().add(dir);
        insertionSort(currentDir.getChildren()); //most of the files are sorted except new one, so can use insertion
        writeFileToDrive(dir);
        Files.write(this.p, this.data);

    }
    /**
     *This method write the string ""New File.\r\n" to the file over and over in each of the clusters until
     * we read the size limit.
     */
<<<<<<< HEAD
=======

>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
    public void writeFileToDrive(Directory dir){
        int size = dir.getSize();
        ArrayList<Integer> dirStarts = new ArrayList<>();
        getDirStarts(dirStarts, firstClusterNumber(dir));

        char[] arr = this.newFileContents.toCharArray();
        int breakpoint = size % bytesPerCluster;
        int pointer = 0;
        for(int i = 0; i < dirStarts.size(); i++){
            for(int j = dirStarts.get(i); j < dirStarts.get(i) + bytesPerCluster; j++){
                if(i == dirStarts.size() - 1){
                    if(j - dirStarts.get(i) == breakpoint) break;
                    data[j] = (byte)arr[pointer];
                }
                else {
                    data[j] = (byte) arr[pointer];
                }
                if(++pointer == 11) pointer = 0;
            }
        }


    }
    /**
     *The method that sets the time and date bytes according to the Fatspec PDF.
<<<<<<< HEAD
     * We only dealt with the "last modified" date and time and left the "created"
     * date and time with 0 bytes.
=======
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
     */
    public void updateTime(byte[] newFile){

        String time = new SimpleDateFormat("HHmmss").format(new Date());
        int h = Integer.valueOf((time.substring(0,2)));
        int hour = ((Integer.valueOf((time.substring(0,2)))) %  24) * 2048;
        int minute = Integer.valueOf(time.substring(2,4)) * 32;
        int second = Integer.valueOf(time.substring(4, 6))/2;

        int finalTime = hour + minute + second;
        byte b1 = (byte)(finalTime % 256);
        byte b2 = (byte)(finalTime / 256);
        newFile[22] = b1;
        newFile[23] = b2;

        String date = new SimpleDateFormat("yyyyddMM").format(new Date());
        int year = (Integer.valueOf(date.substring(0,4)) - 1980) * 512 ;
        int month = Integer.valueOf(date.substring(6,8)) * 32;
        int day = Integer.valueOf(date.substring(4, 6));
        if(h >= 20){
            day++;
        }
        int finalday = year + month + day;
        b1 = (byte)(finalday % 256);
        b2 = (byte)(finalday / 256);
        newFile[24] = b1;
        newFile[25] = b2;

<<<<<<< HEAD
=======



>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
    }
    /**
     *Updates the FAT with info regarding info of the new file.
     * If it spans more clusters, we update each one with the next cluster
     * until the last cluster where we mark it as the end of the file.
     */
    public void updateFAT(int size) throws IOException{
        freeList(); //need to get all the free clusters
        int n = size / bytesPerCluster + ((size % bytesPerCluster == 0) ? 0 : 1);
        if (n > 1){
            //span the clusters then at last cluster put eoc
            int i;
            for(i = 0; i < n; i++) {
                FATOffSet = freeClustersList.get(i) * 4;
                FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
                FATEntOffset = FATOffSet % BPB_BytsPerSec;
                int clusterOffset = FATOffSet + FatTableStart;
                if (i == n - 1) {
                    eocBytesOnFAT(clusterOffset, 4);
                    break;
                }
                addBytesOnFAT(clusterOffset, 4, freeClustersList.get(i + 1));
            }

        }
        else{
            //put eoc
            FATOffSet = freeClustersList.get(0) * 4;
            FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
            FATEntOffset = FATOffSet % BPB_BytsPerSec;
            int clusterOffset = FATOffSet + FatTableStart;
            eocBytesOnFAT(clusterOffset, 4);
        }
    }
    /*
     *Given the cluster number, we determine the low and high bytes
     * for the file. complicated steps to get it in the correct endian-ness.
     */
    public ArrayList<String> getLowHigh(){
        ArrayList<String> list = new ArrayList<>();
        int clusterNumber = freeClustersList.get(0);
        String hex = Integer.toHexString(clusterNumber);
        char[] temp = hex.toCharArray();
        char[] hexes = new char[8];
        for(int i = 0; i < hexes.length; i++){
            hexes[i] = '0';
        }
        StringBuilder high = new StringBuilder();
        StringBuilder low = new StringBuilder();
        int i = temp.length - 1;
        int h = hexes.length - 1;
        while(i > -1){
            hexes[h] = temp[i];
            h--;
            i--;
        }
        for(i = 0; i < 4; i += 2){
            high.insert(0, "" + hexes[i] + hexes[i + 1]);
            String j = "" + hexes[i] + hexes[i + 1];
            list.add(j);

        }
        for(i = 4; i < 8; i += 2){
            low.insert(0, "" + hexes[i] + hexes[i + 1]);
            String k = "" + hexes[i] + hexes[i + 1];
            list.add(k);
        }
        return list;
    }
    /**
     * edits the main byte array of the entire disk with the 64 byte array info
     */
    public int writeFile(byte[] newFile)throws IOException{
<<<<<<< HEAD
        int highest, count = 0;
        ArrayList<Integer> dirStarts = new ArrayList<>();
        N = firstClusterNumber(currentDir);
        getDirStarts(dirStarts, N);
        for(int start : dirStarts) {
            for (int i = start; i < start + bytesPerCluster; i += 64) {
                if (data[i] == -27) {
                    for (int j = 0; j < 64; j++) {
                        data[i] = newFile[j];
                        i++;
                    }
                    return i - 64;
                }
                if (getBytes(i, 4) == 0) {
                    for (int j = 0; j < 64; j++) {
                        data[i] = newFile[j];
                        i++;
                    }
                    return i - 64;
                }
            }
        }
=======
        int highest = 0, count = 0, clusterControl = 64;
        ArrayList<Integer> dirStarts = new ArrayList<>();
        N = firstClusterNumber(currentDir);
        getDirStarts(dirStarts, N);
        int start = dirStarts.get(dirStarts.size() - 1);
            for (int i = start; i < start + bytesPerCluster; i+=64) {
                if(data[i] == -27){
                    for(int j = 0; j < 64; j++){
                        data[i] = newFile[j];
                        i++;
                    }
                    //System.exit(0);
                    return start;
                }
                if(getBytes(i, 4) == 0){
                    for(int j = 0; j < 64; j++){
                        data[i] = newFile[j];
                        i++;
                    }
                    //System.exit(0);
                    return start;
                }
            }
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5

            //if we reach down here it means the current cluster is full and we need a new one
        freeList();
        N = firstClusterNumber(currentDir);
        FATOffSet = N * 4;
        FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
        FATEntOffset = FATOffSet % BPB_BytsPerSec;
        int clusterOffset = FATOffSet + FatTableStart;
        addBytesOnFAT(clusterOffset, 4, freeClustersList.get(0));
        N = freeClustersList.get(0);
        FATOffSet = N * 4;
        FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
        FATEntOffset = FATOffSet % BPB_BytsPerSec;
        clusterOffset = FATOffSet + FatTableStart;
        eocBytesOnFAT(clusterOffset, 4);
        int firstSectorofDirCluster = ((N - 2) * BPB_SecPerClus) + FirstDataSector;
        highest = (firstSectorofDirCluster * BPB_BytsPerSec);
        for(int i = highest; i < highest + 64; i++){
            data[i] = newFile[count];
            count++;
        }
        //System.exit(0);
        return highest;

    }
    /**
     * The first delete method after typing "delete" in the command line.
     */
    public void delete(String name) throws IOException{
        for(int i = 0; i < currentDir.getChildren().size(); i++){
            if(name.equalsIgnoreCase(currentDir.getChildren().get(i).getName())){
                delete(currentDir.getChildren().get(i));
                data[currentDir.getChildren().get(i).getOffsetInParent()] =(byte)229; //set the entry as hidden
                currentDir.getChildren().remove(i); //remove file from tree
                Files.write(p, data);
                freeList(); //updates the new clusters
                return;
            }
        }
        System.out.println("Error: File does not exist!");

    }
    /**
     *This is a complicated method which deletes both files and folders using recursion. If it's a file,
     * delete it and edit the FAT. Otherwise if it's a folder, go into to the folder and recursively delete
     * its contents. For debugging purposes I have the program print the name and type of file delete to
<<<<<<< HEAD
     * see if the recursion is working properly and I believe it does. For example, when I delete "dir", it goes
     * and deletes dir's files as well as "spec" and "fatspec.pdf" (which lie in "a")
=======
     * see if the recursion is working properly and I believe it does.
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
     */
    private void delete(Directory dir){
        if(!dir.isFile()){
            //meaning if dir is a folder, we have to go deeper and delete them too
            if (this.nameMap.add(dir.getName())) {
                getNewFileInfo(dir); // we can skip this step if we already had Cd'ed into that file
            }
            for(Directory d : dir.getChildren()){
                boolean b = d.getName().equals(".")|| d.getName().equals("..");
                if(!b) {
                    delete(d);//the magical statement that goes into the folder and recursively deletes its contents
                }

            }
            //System.out.print("folder deleted: " + dir.getName());
            N = firstClusterNumber(dir);
            deleteClusters(N);
<<<<<<< HEAD
=======
            System.out.println("   " +nameMap.remove(dir.getName()));
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
            return;
        }

        //System.out.println("file deleted: " + dir.getName());
        N = firstClusterNumber(dir);
        deleteClusters(N);
<<<<<<< HEAD
    }

=======
        System.out.println("   " +nameMap.remove(dir.getName()));
    }
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
    /**
     *Another recursive method that deletes contents in the FAT, continuing to the next index
     * if it isn't an EOC.
     */
    private void deleteClusters(int N){
        FATOffSet = N * 4;
        FatSecNum = BPB_RsvdSecCnt + (FATOffSet / BPB_BytsPerSec);
        FatTableStart = FatSecNum * BPB_BytsPerSec;
        FATEntOffset = FATOffSet % BPB_BytsPerSec;
        int clusterOffset = FATEntOffset + FatTableStart;
        int nextClus = getBytes(clusterOffset, 4);
        if(nextClus <= 268435447) {
            zeroOutBytesOnFAT(clusterOffset, 4);
            deleteClusters(nextClus);
        }
        else{
            if(N != 0) {
                zeroOutBytesOnFAT(clusterOffset, 4);
            }
        }

    }
    /**
     *puts 0's in the index and makes sure to edit both FAT tables
     */
    public void zeroOutBytesOnFAT(int offset, int size){
        for(int i = offset; i < offset + size; i++){
            data[i] = (byte) 0;
            data[i + FATSIZE] = (byte) 0; //edit both FATs
        }
    }
    /**
     *puts 0xF8 FF FF 0F in the index to signal an end of cluster
     */
    public void eocBytesOnFAT(int offset, int size){
        byte[] arr = ByteBuffer.allocate(size).putInt(268435448).array();
        int count = 0;
        editBytesOnFAT(offset, size, arr, count);
    }
    /**
     *puts the next cluster in the index
     */
    public void addBytesOnFAT(int offset, int size, int nextCluster)throws IOException{
        byte[] arr = ByteBuffer.allocate(size).putInt(nextCluster).array();
        int count = 0;
        editBytesOnFAT(offset, size, arr, count);

    }
    /**
     *edits both FATs with the new information
     */
    private void editBytesOnFAT(int offset, int size, byte[] arr, int count) {
        for(int i = offset + size - 1; i >= offset; i--){
<<<<<<< HEAD
=======
            //System.out.println(data[i]);
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
            data[i] = arr[count];
            data[i + FATSIZE] = arr[count]; //edit both FATs
            count++;
        }
    }
    //stackoverflow.com/questions/17432738/insertion-sort-using-string-compareto
<<<<<<< HEAD
    /**
     * We use insertion sort whenever we write new file. The reason being that insertion sort is super
     * fast when most of the file is already sorted and all we need to do is sort that new file
     * into place
     */
    private void insertionSort(ArrayList<Directory> arr){
=======
    public void insertionSort(ArrayList<Directory> arr){
>>>>>>> 3c5492886edb5551c964e05078ada061c03573c5
        int i,j;
        Directory key;
        for (j = 1; j < arr.size(); j++) { //the condition has changed
            key = arr.get(j);
            i = j - 1;
            while (i >= 0) {
                if (key.compareTo(arr.get(i)) > 0) {//here too
                    break;
                }
                arr.set(i + 1, arr.get(i));
                i--;
            }
            arr.set(i + 1, key);
        }
    }


}
