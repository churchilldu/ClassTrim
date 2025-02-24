# RefactorByNSGA3

Refactor methods to another class by [NSGA3 or NSGAIII]() following lower [CK metrics]() principle.

## Rule to compute metrics

Strictly follow the tool [CKjm]() used in the paper []().

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


note: when use global variable from other class. Primitive type(int, boolean) can't be traced back. 
See [discussion](https://stackoverflow.com/questions/75954598/how-to-record-visited-constants-by-methodvisitor-in-asm).

### RFC

## QA

1. Why do you visit class files three time?

