<results>
{
for $art in collection('papers')/dblp/article
 where count($art/author) >=2
 and $art/year >= 2000
 return
<publication>
{$art}
</publication>
} </results>