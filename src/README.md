OS FAT32 Project 4 fully completed May 14, 2018 
Group members: Jonah Taurog and Gavriel Baron.

Files: Fat32Reader.java - the main driver of the program and is what implements the commands. 

Directory.java - A file object that contains all the characteristics of each file such as the size and the attribute.

Instructions:

In the terminal we used ```javac *.java``` to compile the classes

and then to run we used ```java -cp Fat32Reader . "abs path to fat32.img"``` to run the program.

To run the commands just type the commands as you would in a command line 
as dictated in Project 3 and 4 specs.

Challenges: This was a very challenging assignment overall. One big issue was figuring out how to use the right endian-ness and "parsing" the bytes to read correctly. That was the building block for finding all the BPB values. Another issue was being able to understand where everything was in the file. Hence, it took a lot of time to craft the loops and to know where to start and end. Yet another challenge was figuring out what to print and what not to print. The secret file was confusing but in the end we found a way to make sure it wasn't printed.
 CD was especially challenging due to issues regarding keeping track of where in the fat32.img we were in.
This led to our idea of having "currentDir" as one of our fields, as it felt like the easiest way to keep track of where we were. It also took a while to figure out how exactly to access the FAT table and read it.

UPDATED CHALLENGES for HW4:
When doing this assignment, we realized that reading the file and flipping through the different clusters that arent' in order was the basis for this assignment. Annoyingly, this made us have to go back and correct our "read" command. Originally it was read by starting at the starting point and reading to the end, it then occured to us that we can't just read straight, because the clusters may not be in order, so we actually had to access each cluster and read the file that way.
It was very challenging to accomplish this.
Another challenge was figuring out how to span a folder to the next cluster upon making a new file (if the current cluster was full.) We had to then make sure that when CD'ing into a new folder that it would go through all the clusters.

A really big challenge was delete. Even though the assignment called for just deleting files, we decided to try and challenge ourselves to deleting both files and folders using recursion. Because we had made our file system using a tree, we were able to then recursively delete folders and everything in them. It used a sort of double recursion because we also have to recursively zero out all the bytes in the indexes of FAT table.

Overall, the FAT project as a whole was challenging because things were built on top of one another, so if we were even one byte off when writing to the drive, it would throw everything off, whether it's editing the FAT, accessing the file locations or reading the files. 


Resources: 
fat32spec.pdf 
https://www.pjrc.com/tech/8051/ide/fat32.html
 https://stackoverflow.com/questions/5886619/hexadecimal-to-integer-in-java
https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
https://stackoverflow.com/questions/17432738/insertion-sort-using-string-compareto

Also came in handy was a hex reader in order to read the bytes in the file. For Mac we used iHex.