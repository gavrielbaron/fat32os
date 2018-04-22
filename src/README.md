OS Project 3 fully completed April 22, 2018 
Group members: Jonah Taurog and Gavriel Baron.

Files: Fat32Reader.java - the main driver of the program and is what implements the commands. 
Directory.java - A file object that contains all the characteristics of each file such as the size and the attribute.

Instructions:

In the terminal we used ```javac *.java``` to compile the classes

and then to run we used ```java -cp Fat32Reader . *path to fat32.img``` to run the program.

To run the commands just type the commands as you would in a command line 
as dictated in Project 3 Use Cases.

Challenges: This was a very challenging assignment overall. One big issue was figuring out how to use the right endian-ness and "parsing" the bytes to read correctly. That was the building block for finding all the BPB values. Another issue was being able to understand where everything was in the file. Hence, it took a lot of time to craft the loops and to know where to start and end. Yet another challenge was figuring out what to print and what not to print. The secret file was confusing but in the end we found a way to make sure it wasn't printed.
 CD was especially challenging due to issues regarding keeping track of where in the fat32.img we were in.
This led to our idea of having "currentDir" as one of our fields, as it felt like the easiest way to keep track of where we were. It also took a while to figure out how exactly to access the FAT table and read it. 

Resources: fat32spec.pdf https://www.pjrc.com/tech/8051/ide/fat32.html https://stackoverflow.com/questions/24423816/convert-unicode-string-to-ascii-in-java-which-works-in-unix-linux https://stackoverflow.com/questions/5886619/hexadecimal-to-integer-in-java

Also came in handy was a hex reader in order to read the bytes in the file. For the Mac we used iHex.