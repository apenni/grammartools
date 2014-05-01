Stanford POS Tagger, v. 1.5 - 13 May, 2008
Copyright (c) 2002-2008 The Board of Trustees of
The Leland Stanford Junior University. All Rights Reserved.

This document contains (some) information about the models included in
this release and that may be downloaded for the POS tagger website at
http://nlp.stanford.edu/software/tagger.shtml .  If you have downloaded
the full tagger, all of the models mentioned in this document are in the
downloaded package in the same directory as this readme.  Otherwise,
included in the download are the two non-distributional similarity
English taggers, and the other taggers may be downloaded from the
website.  All taggers are accompanied by the props files used to create
them; please examine these files for more detailed information about the
creation of the taggers.

For English, the bidirectional taggers is slightly more accurate, but
tags more slowly; choose the appropriate tagger based on your
speed/performance needs.

English taggers
---------------------------
bidirectional-wsj-0-18.tagger
Trained on WSJ sections 0-18 using a bidirectional architecture and
includes word shape features.  Penn tagset.
Performance:
97.18% correct on WSJ 19-21
(89.30% correct on unknown words)

left3words-wsj-0-18.tagger
Trained on WSJ sections 0-18 using the left3words architectures and
includes word shape features.  Penn tagset.
Performance:
96.97% correct on WSJ 19-21
(89.03% correct on unknown words)

left3words-distsim-wsj-0-18.tagger
Trained on WSJ sections 0-18 using the left3words architectures and
includes word shape and distributional similarity features. Penn tagset.
Performance:
96.99% correct on WSJ 19-21
(89.77% correct on unknown words)


Chinese tagger
---------------------------
chinese.tagger
Trained on a combination of texts from Chinese and Hong Kong sources.
Performance:
93.60% on a combination of Chinese and Hong Kong texts
(81.61% on unknown words)

Arabic tagger
---------------------------
arabic.tagger
Trained on the train part of the ATB as split for the 2005 JHU Summer Workshop,
using Bies tags (data distributed by Mona Diab).
Performance:
96.72% on dev portion according to Diab split
(77.49% on unknown words)


German tagger
---------------------------
Trained on the first 80% of the Negra corpus, which uses the STTS tagset.
The Stuttgart-Tübingen Tagset (STTS) is a set of 54 tags for annotating
German text corpora with part-of-speech labels, which was jointly
developed by the Institut für maschinelle Sprachverarbeitung of the
University of Stuttgart and the Seminar für Sprachwissenschaft of the
University of Tübingen. See: 
http://www.ims.uni-stuttgart.de/projekte/CQPDemos/Bundestag/help-tagset.html
Performance:
94.49% on the first half of the remaining 20% of the Negra corpus
(80.28% on unknown words)
