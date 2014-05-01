% GrammarTools
% This file contains prolog DCG's for parsing sentences
% Each clause and terms represents a part of speech phrase or tag

% Sentence
root --> s.
root --> [root].
top --> root.
top --> [top].

% Clausal
s --> np, vp.
s --> np, vp, end.
s --> vp.
s --> vp, end.
s --> s2, colon, s2, end.
s --> s2, comma, s2, end.
s --> [s].
s2 --> [s].

% Phrasal
np --> nn.
np --> dt.
np --> prp.
np --> nnp.
np --> nns.
np --> nnps.
np --> dt, nn.
np --> dt, jj, nn.
np --> dt, nns.
np --> dt, nnp.
np --> dt, nn, nn.
np --> dt, nnp, nnp.
np --> 'prp$', nn.
np --> cd.
np --> np2, cc, np2.
np --> jj, nn.
np --> jj, nns.
np --> np2, pp.
np --> np2, vp.
np --> [np].
np2 --> [np].
% use the above replacement rule to avoid endless recursive substitutions
% np --> np, pp.

vp --> to, vp.
vp --> vb, np.
vp --> md, vp.
vp --> adv, vbz.
vp --> vbz, np.
vp --> vbd, np.
vp --> vbg, np.
vp --> vbn, np.
vp --> vbp, np.
vp --> vbz, pp.
vp --> [vp].

pp --> in, np.
pp --> [pp].

% Word
cc --> [cc].
cd --> [cd].
dt --> [dt].
ex --> [ex].
fw --> [fw].
in --> [in].
jj --> [jj].
jjr --> [jjr].
jjs --> [jjs].
ls --> [ls].
md --> [md].
nn --> [nn].
nns --> [nns].
nnp --> [nnp].
nnps --> [nnps].
pdt --> [pdt].
pos --> [pos].
prp --> [prp].
'prp$' --> ['prp$'].
rb --> [rb].
rbr --> [rbr].
rbs --> [rbs].
rp --> [rp].
sym --> [sym].
to --> [to].
uh --> [uh].
vb --> [vb].
vbd --> [vbd].
vbg --> [vbg].
vbn --> [vbn].
vbp --> [vbp].
vbz --> [vbz].
wdt --> [wdt].
wp --> [wp].
'wp$' --> ['wp$'].
wrb --> [wrb].

% Functional
adv --> [adv].
nom --> [nom].

% Punctuational
% punctuation makrs will be expressed as literals by
% their hexidecimal character code prefixed by an 'x'
end --> ['.'].
comma  --> [','].
colon --> [':'].
hyphen --> ['-'].