<results> {
let $auction := doc("xmark100m.xml") return
 for $i in $auction/site//item
 where contains(string(exactly-one($i/description)), "gold")
 return <item_desc_gold> { $i/name/text() } </item_desc_gold>
} </results>