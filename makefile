JAVAC=/usr/bin/javac
JFLAGS = -g
DOC=.

.SUFFIXES: .java .class

SRCDIR=src
BINDIR=bin



$(BINDIR)/%.class:$(SRCDIR)/%.java
	$(JAVAC) -d $(BINDIR)/ -cp $(BINDIR) $<

CLASSES =  MessageHeader.class MessageBody.class Message.class EAD.class registerGUI.class userGUI.class ClientGUI.class Client.class User.class Server.class

CLASS_FILES=$(CLASSES:%.class=$(BINDIR)/%.class)


default: class

class: 
	javac -d bin src/*.java


classes: $(CLASS_FILES)

doc: 
	javadoc -d doc/ src/*.java

clean:
	rm $(BINDIR)/*.class
	rm -r doc/
