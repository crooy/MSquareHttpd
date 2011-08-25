SCALA=scala
SCALAC=scalac
SCALADOC=scaladoc	

BIN=bin
SRC=src
DOC=doc


# Shortcuts.
default: $(BIN)/M2HTTPD.class

all: $(BIN)/M2HTTPD.class $(DOC)/index.html

docs: $(DOC)/index.html

zip: M2HTTPD.zip


# Rules.
$(BIN)/M2HTTPD.class: $(BIN) $(SRC)/M2HTTPD.scala 
	$(SCALAC) -d $(BIN) $(SRC)/M2HTTPD.scala 

$(DOC)/index.html: $(DOC) $(SRC)/M2HTTPD.scala 
	$(SCALADOC) -d $(DOC) $(SRC)/M2HTTPD.scala 

$(BIN):
	mkdir $(BIN)

$(DOC):
	mkdir $(DOC)


# Execute the web server.
run: $(BIN)/M2HTTPD.class
	$(SCALA) -cp $(BIN) SimpleHTTPD


# Package this directory up.
M2HTTPD.zip: 
	zip -r M2HTTPD.zip .


# Delete class files and generated documentation.
clean:
	rm -fv *~
	rm -rvf $(BIN) $(DOC) M2HTTPD.zip 
	rm -fv src/*~
	rm -fv www/*~
