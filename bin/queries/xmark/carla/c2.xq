<results>
{
for $c in doc('dblp.xml')/dblp/inproceedings
 return
<inproceeding>
{$c/title}
</inproceeding>
} </results>