% GrammarTools
% This file contains prolog DCG's for parsing sentences
% Each clause and terms represents a part of speech phrase or tag
% Constraints on clauses are tense (T) and number (N),
% present, past or future (r,p,f) and number (N), singular or plural (s,p) (respectively).

% Sentence
root --> s.
root --> [root].
top --> root.
top --> [top].

% Clausal
s --> np(T,N), vp(T,N).
s --> np(T,N), vp(T,N), end.
s --> vp(_,_).
s --> vp(_,_), end.
s --> s2, colon, s2, end.
s --> s2, comma, s2, end.
s --> [s].
s2 --> [s].

% Phrasal
np(_,_) --> nn.
np(_,_) --> dt.
np(_,_) --> prp.
np(_,s) --> nnp.
np(_,p) --> nns.
np(_,p) --> nnps.
np(_,_) --> dt, nn.
np(_,_) --> dt, jj, nn.
np(_,p) --> dt, nns.
np(_,s) --> dt, nnp.
np(_,_) --> dt, nn, nn.
np(_,s) --> dt, nnp, nnp.
np(_,_) --> 'prp$', nn.
np(_,_) --> cd.
np(_,_) --> np2(_,_), cc, np2(_,_).
np(_,_) --> jj, nn.
np(_,p) --> jj, nns.
np(T,N) --> np2(T,N), pp.
np(T,N) --> np2(T,N), vp(T,N).
np(_,_) --> [np].
np2(_,_) --> [np].
% use the above replacement rule to avoid endless recursive substitutions
% np --> np, pp.

vp(r,N) --> to, vp(r,N).
vp(r,N) --> vb, np(r,N).
vp(r,s) --> vbp, vp(r,s).
vp(r,N) --> md, vp(r,N).
vp(r,s) --> vbz, vp(_,_).
vp(r,s) --> adv, vp(r,s).
vp(r,s) --> vbz, np(r,s).
vp(p,N) --> vbd, np(p,N).
vp(r,N) --> vbg, np(r,N).
vp(p,N) --> vbn, np(p,N).
vp(r,s) --> vbp, np(r,s).
vp(r,s) --> vbz, pp(r,s).
vp(p,N) --> vbd, pp(p,N).
vp(r,_) --> vbg, pp(r,_).
vp(_,_) --> vbd, rb, np(_,_).
%vp(p,_) --> (rb|), vbd, (rb|), np(_,_), (rb|).
vp(_,_) --> [vp].

pp(T,N) --> in, np(T,N).
pp(T,N) --> to, np(T,N).
pp(_,_) --> [pp].

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
% punctuation marks will be expressed as literals by
% their hexidecimal character code prefixed by an 'x'
end --> ['.'].
end --> ['?'].
end --> ['!'].
comma  --> [','].
colon --> [':'].
hyphen --> ['-'].
