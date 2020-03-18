JC=javac
JR=jar cvfm
RM=rm -f

all: classes jar clean

classes:
	find ./tr -name "*.java" > sources.txt
	$(JC) @sources.txt
	$(RM) sources.txt

jar:
	$(JR) logisim-ceng.jar META-INF/MANIFEST.MF .

clean:
	for class_file in `find ./tr -name "*.class"`; do \
		$(RM) $$class_file; \
	done

.PHONY: clean