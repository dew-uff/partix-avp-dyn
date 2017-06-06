<results> {
let $auction := doc("xmark100m.xml") return
 for $b in $auction/site/regions//item
 let $k := $b/name/text()
 order by zero-or-one($b/location) ascending empty greatest
 return <items> { <item name="{$k}">{$b/location/text()}</item> } </items>
} </results>