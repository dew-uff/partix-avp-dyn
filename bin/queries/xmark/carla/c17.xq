<results>{
for $at in collection('papers')/dblp/article
 where count($at/author) > 2
 return
<article>
{$at/title}
{$at/author}
{$at/year}
{$at/journal}
{$at/number}
{$at/pages}
<qtde_authors>{count($at/author)}</qtde_authors>
</article>
}</results>