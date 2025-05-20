# RefactorByNSGA3

Refactor methods to another class by [NSGA3 or NSGAIII]() (Non-dominated Sorting Genetic Algorithm) 
following lower [CK metrics]() principle.

## Code features

1. Use [lombok]() to really simplify the data class code (getter/setter, constructor...)
2. Use Java8 new feature [Optional]() to handle null pointer elegantly.
3. Consider immutability, return unmodified list.
4. Cache the object to improve performance.
5. Reduce the dependencies as much as possible.
6. Auto record the experiment results and summary.

## Rule to compute metrics

Strictly follow the tool [CKjm]() used in the paper []().

Where are the parsed classes and methods possibly from?
1. JDK
2. 3rd Libraries.
3. Local project.

### WMC

WMC = the number of declaring methods.

### CBO

CBO = size of set of following classes (exclude class from jdk) :
1. super class
2. interface
3. Field type 
4. Declaring method Exception
5. Declaring method arguments type
6. Declaring method return type 
7. Field from other class 
8. invoked methods' class


note: when using global variable from other class. Primitive types (int, boolean) can't be traced back. 
See [discussion](https://stackoverflow.com/questions/75954598/how-to-record-visited-constants-by-methodvisitor-in-asm).

### RFC

## QA

1. Why do you visit class files three times?

2. Why don't you use a database instead of files to record all the info?

It may be improved by using a database.
I didn't intend to bring in the database in the program design. 
Because it didn't have lots of data to store, And I need to design a lot of tables.
So I choose the tsv file (csv comma conflict with method signature) to store the information and serialize the object for the future.
But it has a little inconvenience on summarizing and processing data; 
Maybe the database is a better choice, I didn't want to change it kind of as long as it works.