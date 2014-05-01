@echo off
rem Generates feature data from a data set of sentences

rem n = number of errors per sentence
rem TODO: extract this from the folder name
set n=%1

rem in = input path, out = output file (input output should be dumped into the input folder name
rem TODO: input is taken from each folder, output dumped back to that folder
rem ideally, output filename is "corpus.errortype.n.arff", but it's also sufficient to just name it "sentences.arff"
set in=%2
set out=%3

rem heap memory, more is better!
set mem=4096m

java -Xmx%mem% -jar "dist.dataset\GrammarTools.DataSetConsoleApp.jar" %n% %in% %out%

rem examples
rem run.dataset.bat 0 "data\...\orig" "data\...\orig\...orig.arff"
rem run.dataset.bat 1 "data\...\tense\1" "data\...\orig\...tense.1.arff"