ifeq ($(OS),Windows_NT)
	RM = rmdir /s /q
	CP = copy /Y
else
	RM = rm -rf
	CP = cp -f
endif

sources = $(wildcard *.java)
classes = $(sources:.java=.class)

SRC_DIR = ./src/main/java

PACKAGE = com.segc
CLIENT = $(subst .,/,$(PACKAGE))/Tintolmarket.java
SERVER = $(subst .,/,$(PACKAGE))/TintolmarketServer.java

JFLAGS = -cp "." -g -sourcepath "./src/main/java" -d
.SUFFIXES: .java .class
.java.class:
	javac $(JFLAGS) $(BUILD_DIR) $*.java

default: client server

client: BUILD_DIR = "./target/client"
client: $(SRC_DIR)/$(CLIENT:.java=.class)
	cd $(BUILD_DIR) && jar cvfe $@.jar $(PACKAGE).Tintolmarket .

server: BUILD_DIR = "./target/server"
server: $(SRC_DIR)/$(SERVER:.java=.class)
	cd $(BUILD_DIR) && jar cvfe $@.jar $(PACKAGE).TintolmarketServer .

clean:
	${RM} "./target"