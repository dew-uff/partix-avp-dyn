<results> {
let $auction := doc("xmark100m.xml") return
 for $i in $auction/site/regions/australia/item
 return <item name="{$i/name/text()}">{$i/description}</item>
} </results>