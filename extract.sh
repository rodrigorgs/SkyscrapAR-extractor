#unlink git-repos
#ln -s $1 git-repos
java -cp bin:lib/javagit-0.1.0.jar:lib/prefuse.jar:lib/jdom.jar GitExtractor
