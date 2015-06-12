ANTLR4=antlr4
ANTLRFLAGS=-no-listener -visitor
JAVAC=javac
CC=g++
CCFLAGS=-std=c++11 -o maschine

ifeq ($(OS),Windows_NT)
	CCFLAGS+=-lws2_32
	EXEC=maschine.exe
else
	EXEC=maschine
endif

all: vm assembler

vm:
	$(CC) maschine.cpp $(CCFLAGS)

assembler: antlr
	$(JAVAC) *.java

antlr:
	$(ANTLR4) $(ANTLRFLAGS) LittleMaschine.g4

clean:
	rm *.class *.tokens $(EXEC)
	rm LittleMaschineBaseVisitor.java LittleMaschineLexer.java
	rm LittleMaschineParser.java LittleMaschineVisitor.java
