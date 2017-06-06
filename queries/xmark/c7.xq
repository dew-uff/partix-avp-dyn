<results> {
let $auction := doc("xmark100m.xml") return
 for $p in $auction/site
 return
  <total_desc_anno_email_elements> { count($p//description) + count($p//annotation) + count($p//emailaddress) } </total_desc_anno_email_elements>
} </results>
