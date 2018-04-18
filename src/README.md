OS Project 3 halfway submission
April 15, 2018
Group members: Jonah Taurog and Gavriel Baron.

Files:
Fat32Reader.java - the main driver of the program and is what implements the commands. 
Directory.java - it's basically a file object that contains all the characteristics of each file like the size and the attribute

Instructions:
In the terminal we used ```javac *.java``` to compile 

and then to run we used ```java -cp Fat32Reader . (path to the image)```

So far we only have only info, ls, and stat implemented.

To run the commands all you have to do is type ```ls``` or ```info``` or ```stat *name of file*```


Challenges:
This was a very challenging assigment so far. One big issue was figuring out how to use the right endian-ness and "parsing" the bytes to read correctly. That was the building block for finding all the BPB values.
 Another issue was being able to understand where everything was in the file. Hence, it took a lot of time to craft the loops and to know where to start and end. Yet another challange was figuring out what to print and what not to print. The secret file was confusing but in the end
we found a way to make sure it wasn't printed.


Resources:
fat32spec.pdf
https://www.pjrc.com/tech/8051/ide/fat32.html
https://stackoverflow.com/questions/24423816/convert-unicode-string-to-ascii-in-java-which-works-in-unix-linux
https://stackoverflow.com/questions/5886619/hexadecimal-to-integer-in-java

Also came in handy was a hex reader in order to read the bytes in the file. For the Mac we used iHex. 