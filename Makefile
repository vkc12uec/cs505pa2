all:
	javac *.java
	rmic Locks


clean:
	rm *.class
