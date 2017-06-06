<results> {
let $auction := doc("xmark100m.xml") return
 for $p in $auction/site/people/person
 where empty($p/homepage/text())
 return <persons_without_homepage> { <person name="{$p/name/text()}"/> } </persons_without_homepage>
} </results>