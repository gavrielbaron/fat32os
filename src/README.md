OS Project 3 halfway submission
April 15, 2018
Group members: Jonah Taurog and Gavriel Baron.

Files:
Fat32Reader.java - the main driver of the program and is what implements the commands. 
Directory.java - A file object that contains all the characteristics of each file such as the size and the attribute.

Instructions:

In the terminal we used ```javac *.java``` to compile 

and then to run we used ```java -cp Fat32Reader . (path to the image)```


To run the commands just type the commmands as you would in a command line


Challenges:
This was a very challenging assigment so far. One big issue was figuring out how to use the right endian-ness and "parsing" the bytes to read correctly. That was the building block for finding all the BPB values.
Another issue was being able to understand where everything was in the file. Hence, it took a lot of time to craft the loops and to know where to start and end. Yet another challange was figuring out what to print and what not to print. The secret file was confusing but in the end we found a way to make sure it wasn't printed.
CD was escpecially challenging due to issues regarding keeping track of where in the fat32.img we were.


Resources:
fat32spec.pdf
https://www.pjrc.com/tech/8051/ide/fat32.html
https://stackoverflow.com/questions/24423816/convert-unicode-string-to-ascii-in-java-which-works-in-unix-linux
https://stackoverflow.com/questions/5886619/hexadecimal-to-integer-in-java

Also came in handy was a hex reader in order to read the bytes in the file. For the Mac we used iHex. 
