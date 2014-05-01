#!/bin/sh
#Generates feature data from a data set of sentences

#n = number of errors per sentence 
#TODO: extract this from the folder name
n=$1

#in = input path, out = output file (input output should be dumped into the input folder name
#TODO: input is taken from each folder, output dumped back to that folder
#ideally, output filename is "corpus.errortype.n.arff", but it's also sufficient to just name it "sentences.arff"
in=$2
out=$3

#heap memory, more is better!
mem=4096m

java -Xmx$mem -jar "dist.dataset/GrammarTools.DataSetConsoleApp.jar" $n $in $out

# examples
# run.dataset.sh 0 "data\...\orig" "data\...\orig\...orig.arff"
# run.dataset.sh 1 "data\...\tense\1" "data\...\orig\...tense.1.arff"